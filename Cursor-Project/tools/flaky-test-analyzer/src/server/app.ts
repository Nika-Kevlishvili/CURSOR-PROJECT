import fs from 'node:fs';
import path from 'node:path';
import express, { type ErrorRequestHandler, type NextFunction, type Request, type Response } from 'express';
import cors from 'cors';
import multer from 'multer';
import { z } from 'zod';
import { ENVIRONMENTS } from '../shared/types.js';
import type { AppContext } from './context.js';
import { HttpError } from './errors.js';
import { importReport, reparseStoredReports } from './services/importService.js';
import { compareTests, dashboard, getProposal, listProposals, listReports, listResults, resultDetail, type ListFilters } from './services/queryService.js';
import { applyApproved, decideProposal } from './services/proposalService.js';
import { validateProposal, validationResult } from './services/validationService.js';

const environmentSchema = z.enum(ENVIRONMENTS);

function asyncRoute(handler: (req: Request, res: Response, next: NextFunction) => Promise<void>) {
  return (req: Request, res: Response, next: NextFunction) => {
    handler(req, res, next).catch(next);
  };
}

function isAllowedOrigin(origin: string | undefined, allowed: string[]): boolean {
  if (!origin) return true;
  if (allowed.includes(origin)) return true;
  try {
    const url = new URL(origin);
    const loopback = url.hostname === '127.0.0.1' || url.hostname === 'localhost' || url.hostname === '[::1]';
    return loopback && url.protocol === 'http:';
  } catch {
    return false;
  }
}

function requireMutationHeader(req: Request, res: Response, next: NextFunction) {
  if (req.method === 'GET' || req.method === 'HEAD' || req.method === 'OPTIONS') {
    next();
    return;
  }
  if (req.header('x-flaky-analyzer') !== '1') {
    res.status(403).json({
      error: 'Missing X-Flaky-Analyzer header. Opening a page does not authorize a change.',
      code: 'ORIGIN_GUARD',
    });
    return;
  }
  next();
}

function filtersFrom(query: Request['query']): ListFilters {
  const environment = typeof query.environment === 'string' && environmentSchema.safeParse(query.environment).success
    ? environmentSchema.parse(query.environment)
    : undefined;
  const groupRaw = typeof query.groupBy === 'string' ? query.groupBy : undefined;
  const groupBy: ListFilters['groupBy'] = groupRaw === 'signature' || groupRaw === 'file' || groupRaw === 'endpoint' || groupRaw === 'selector' || groupRaw === 'cause'
    ? groupRaw
    : undefined;
  return {
    environment,
    from: typeof query.from === 'string' ? query.from : undefined,
    to: typeof query.to === 'string' ? query.to : undefined,
    status: typeof query.status === 'string' ? query.status : undefined,
    rootCause: typeof query.rootCause === 'string' ? query.rootCause : undefined,
    fixStatus: typeof query.fixStatus === 'string' ? query.fixStatus : undefined,
    q: typeof query.q === 'string' ? query.q : undefined,
    groupBy,
    attention: query.attention === '1',
    limit: typeof query.limit === 'string' ? Number(query.limit) : undefined,
    offset: typeof query.offset === 'string' ? Number(query.offset) : undefined,
  };
}

export function createApp(ctx: AppContext) {
  const app = express();
  app.disable('x-powered-by');
  app.use(cors({
    origin(origin, callback) {
      if (isAllowedOrigin(origin, ctx.config.allowedOrigins)) {
        callback(null, true);
        return;
      }
      callback(new HttpError(403, 'Origin is not allowed.', 'ORIGIN_FORBIDDEN'));
    },
    allowedHeaders: ['Content-Type', 'X-Flaky-Analyzer'],
  }));
  app.use(express.json({ limit: '1mb' }));
  app.use('/api', requireMutationHeader);

  const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: ctx.config.limits.maxArchiveBytes, files: 1 },
  });

  app.get('/api/health', (_req, res) => {
    res.json({
      ok: true,
      automationRoot: ctx.config.automationRoot,
      automationRootExists: fs.existsSync(ctx.config.automationRoot),
      aiEnabled: ctx.advisor.enabled,
      host: ctx.config.host,
      retentionDays: ctx.config.retentionDays,
    });
  });

  app.get('/api/dashboard', (req, res) => {
    res.json(dashboard(ctx, filtersFrom(req.query)));
  });

  app.get('/api/reports', (_req, res) => {
    res.json({ reports: listReports(ctx) });
  });

  app.get('/api/reports/:id', (req, res) => {
    const reports = listReports(ctx);
    const report = reports.find((item) => item.id === req.params.id);
    if (!report) throw new HttpError(404, 'Report was not found.', 'NOT_FOUND');
    res.json({ report });
  });

  app.post('/api/reports/import', upload.single('report'), asyncRoute(async (req, res) => {
    const file = req.file;
    if (!file) throw new HttpError(400, 'Choose a ZIP file to import.', 'FILE_REQUIRED');
    const environment = environmentSchema.safeParse(req.body?.environment);
    if (!environment.success) throw new HttpError(400, 'Select Dev, Dev2, or Test for this execution.', 'BAD_ENVIRONMENT');
    const label = typeof req.body?.label === 'string' ? req.body.label : null;
    const executedAt = typeof req.body?.executedAt === 'string' && req.body.executedAt ? req.body.executedAt : null;
    const result = await importReport(ctx, {
      bytes: new Uint8Array(file.buffer),
      filename: file.originalname,
      environment: environment.data,
      label,
      executedAt,
    });
    res.status(result.duplicate ? 200 : 201).json(result);
  }));

  app.post('/api/reports/reanalyze', asyncRoute(async (_req, res) => {
    res.json(reparseStoredReports(ctx));
  }));

  app.get('/api/results', (req, res) => {
    res.json(listResults(ctx, filtersFrom(req.query)));
  });

  app.get('/api/results/:id', (req, res) => {
    res.json(resultDetail(ctx, req.params.id));
  });

  app.get('/api/compare', (req, res) => {
    const testId = typeof req.query.testId === 'string' ? req.query.testId : undefined;
    res.json(compareTests(ctx, testId));
  });

  app.get('/api/proposals', (req, res) => {
    const state = typeof req.query.state === 'string' ? req.query.state : undefined;
    res.json({ proposals: listProposals(ctx, state) });
  });

  app.get('/api/proposals/:id', (req, res) => {
    res.json(getProposal(ctx, req.params.id));
  });

  app.post('/api/proposals/:id/decision', asyncRoute(async (req, res) => {
    const body = z.object({
      decision: z.enum(['approve', 'approve_and_apply', 'reject', 'revert', 'reanalyze']),
      proposalId: z.string().min(1),
      note: z.string().max(1000).optional(),
    }).parse(req.body);
    res.json(decideProposal(ctx, req.params.id, body.decision, body.proposalId, body.note));
  }));

  app.post('/api/proposals/:id/apply', asyncRoute(async (req, res) => {
    const body = z.object({ proposalId: z.string().min(1) }).parse(req.body);
    if (body.proposalId !== req.params.id) {
      throw new HttpError(400, 'The proposal id must match the approved proposal. No file was changed.', 'PROPOSAL_MISMATCH');
    }
    res.json(applyApproved(ctx, req.params.id));
  }));

  app.post('/api/proposals/:id/validate', asyncRoute(async (req, res) => {
    const body = z.object({
      proposalId: z.string().min(1),
      environment: environmentSchema,
      repetitions: z.number().int().optional(),
      confirm: z.literal(true),
    }).parse(req.body);
    res.json(await validateProposal(ctx, req.params.id, body));
  }));

  app.get('/api/validations/:id', (req, res) => {
    res.json(validationResult(ctx, req.params.id));
  });

  const clientDir = path.join(ctx.config.appRoot, 'dist', 'client');
  if (fs.existsSync(clientDir)) {
    app.use(express.static(clientDir));
    app.use((req, res, next) => {
      if (req.method !== 'GET' || req.path.startsWith('/api/')) {
        next();
        return;
      }
      res.sendFile(path.join(clientDir, 'index.html'));
    });
  }

  const errorHandler: ErrorRequestHandler = (error, _req, res, _next) => {
    if (error instanceof multer.MulterError && error.code === 'LIMIT_FILE_SIZE') {
      res.status(413).json({ error: 'The ZIP file exceeds the configured archive size limit.', code: 'ARCHIVE_TOO_LARGE' });
      return;
    }
    if (error instanceof z.ZodError) {
      res.status(400).json({ error: 'The request is missing a required field or has the wrong type.', code: 'BAD_REQUEST', details: error.issues.map((issue) => issue.message) });
      return;
    }
    if (error instanceof HttpError) {
      res.status(error.status).json({ error: error.message, code: error.code });
      return;
    }
    console.error(error instanceof Error ? error.message : 'Unhandled analyzer error');
    res.status(500).json({ error: 'The analyzer hit an unexpected error.', code: 'INTERNAL' });
  };
  app.use(errorHandler);
  return app;
}
