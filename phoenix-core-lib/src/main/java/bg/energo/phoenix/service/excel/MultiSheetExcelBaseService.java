package bg.energo.phoenix.service.excel;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class MultiSheetExcelBaseService {

    private static final String APP_NAME = "EPB";

    /**
     * @return the type of multi-sheet Excel workbook to generate
     */
    public abstract MultiSheetExcelType getMultiSheetExcelType();

    /**
     * @return the batch size (records per sheet) to use when generating an Excel workbook
     */
    public abstract int getBatchSize();

    /**
     * Returns a list of data to be used for generating an Excel workbook.
     *
     * @param args the arguments to use when retrieving data
     * @return a list of data to be used for generating an Excel workbook
     */
    public abstract List<?> getData(String... args);

    /**
     * @return a list of headers to be used for generating an Excel workbook
     */
    public abstract List<String> getHeaders();

    /**
     * Fills a row in an Excel worksheet with data from a single record.
     *
     * @param ws     the Excel worksheet to fill a row in
     * @param record the data record to use for filling the row
     * @param rowNum the row number in the worksheet to fill with the record data
     * @param <T>    the type of data record being filled
     */
    public abstract <T> void fillRow(Worksheet ws, T record, int rowNum);

    /**
     * Generates an Excel workbook with data retrieved from the {@link #getData} method, using a specified
     * batch size (records per sheet) and number of threads to process the data. The generated workbook is streamed to the
     * provided {@link HttpServletResponse} object's output stream.
     *
     * @param response the HTTP response object to stream the generated Excel workbook to
     * @param args     the arguments to pass to the {@link #getData} method for retrieving input data to export
     */
    @SneakyThrows
    public void generateExcel(HttpServletResponse response, String... args) {
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            processExcelGeneration(outputStream, args);
        }
    }


    /**
     * Generates an Excel file and writes it to the given output stream. The data and structure
     * of the Excel file are determined by the provided arguments.
     *
     * @param outputStream the output stream where the generated Excel file will be written
     * @param args         optional arguments used for customizing the Excel generation process
     */
    @SneakyThrows
    public void generateExcel(OutputStream outputStream, String... args) {
        try (outputStream) {
            processExcelGeneration(outputStream, args);
        }
    }

    private void processExcelGeneration(OutputStream outputStream, String[] args) throws InterruptedException, ExecutionException, IOException {
        // Retrieve input data that needs to be exported
        List<?> records = getData(args);

        // Determine the number of threads to use based on the size of the input list
        int threadCount = 1; // Set the default thread count to 1
        int batchSize = getBatchSize();
        if (records.size() > getBatchSize()) { // Check if there are more records than the batch size
            threadCount = records.size() / batchSize; // Calculate the number of threads needed to process all records
            if (records.size() % batchSize != 0) {
                threadCount++; // If there are remaining records, add an extra thread to process them
            }
        }

        // Create a new Workbook instance and array of CompletableFutures for the asynchronous generation
        Workbook workbook = new Workbook(outputStream, APP_NAME, null);
        CompletableFuture<?>[] cfs = new CompletableFuture<?>[threadCount];

        // For each thread, create a worksheet and a CompletableFuture to generate the workbook asynchronously
        for (int i = 0; i < threadCount; i++) {
            int from = i * batchSize;
            int to = Math.min(from + batchSize, records.size());
            log.debug("index: " + i + ", from: " + from + ", to:" + to);
            Worksheet ws = workbook.newWorksheet("Sheet " + (i + 1));
            CompletableFuture<Void> cf = getAsyncCompletableFuture(records, from, to, ws, i);
            cfs[i] = cf;
        }

        // Wait for all the CompletableFutures to complete and finish the workbook
        CompletableFuture.allOf(cfs).get();
        workbook.finish();
    }

    /**
     * Creates and returns a {@link CompletableFuture} that processes a subset of the given list of records in a separate thread,
     * generating data in the given worksheet for the corresponding index.
     *
     * @param records the list of records to process
     * @param from    the starting index (inclusive) of the sublist of records to process
     * @param to      the ending index (exclusive) of the sublist of records to process
     * @param ws      the worksheet to generate data in
     * @param index   the index of the worksheet, used for generating a sheet name
     * @return a {@link CompletableFuture} representing the asynchronous processing of the sublist of records in the given worksheet
     */
    public <T> CompletableFuture<Void> getAsyncCompletableFuture(List<T> records, int from, int to, Worksheet ws, int index) {
        return CompletableFuture.runAsync(() -> processSheet(records.subList(from, to), ws, index));
    }

    /**
     * Generates data in the given worksheet for the given list of records, with the corresponding index used for logging.
     *
     * @param records the list of records to generate data for
     * @param ws      the worksheet to generate data in
     * @param index   the index of the worksheet, used for logging
     * @param <T>     the type of records in the list
     */
    private <T> void processSheet(List<T> records, Worksheet ws, int index) {
        setHeader(ws, getHeaders());

        long startTime = System.currentTimeMillis();
        fillRows(ws, records);
        long estimatedTime = System.currentTimeMillis() - startTime;

        log.debug("Process " + index + " writing took: " + TimeUnit.MILLISECONDS.toSeconds(estimatedTime) + " seconds");
    }

    /**
     * Sets the headers of the given worksheet with the given list of header strings.
     *
     * @param ws      the worksheet to set headers for
     * @param headers the list of header strings to set as column headers
     */
    private void setHeader(Worksheet ws, List<String> headers) {
        int colNum = 0;
        for (String h : headers) {
            ws.value(0, colNum++, h);
        }
    }

    /**
     * Fills the rows of the given worksheet with data from the given list of records, starting from the second row.
     *
     * @param ws      the worksheet to fill with data
     * @param records the list of records to retrieve data from
     * @param <T>     the type of records in the list
     */
    private <T> void fillRows(Worksheet ws, List<T> records) {
        int rowNum = 1;
        for (T record : records) {
            fillRow(ws, record, rowNum);
            rowNum++;
        }
    }
}
