import json, re
p = r'c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\swagger\dev2\swagger-spec.json'
with open(p, encoding='utf-8') as f:
    data = json.load(f)
paths = data.get('paths', {})
for path in sorted(paths):
    if 'product-contract' in path or 'resign' in path.lower():
        for method, op in paths[path].items():
            if method in ('get','post','put','patch','delete'):
                print(f'{method.upper():6} {path}  {op.get("operationId","")}')
schemas = data.get('components', {}).get('schemas', {})
for name in sorted(schemas):
    if 'resign' in name.lower() or 'Resign' in name:
        print('SCHEMA', name)
for name in sorted(schemas):
    if name in ('ProductContractCreateRequest','ProductContractUpdateRequest','ProductParameterBaseRequest','ProductContractRequest'):
        props = schemas[name].get('properties', {})
        for k in props:
            if 'resign' in k.lower() or 'wait' in k.lower() or 'sign' in k.lower():
                print(name, k, props[k])
