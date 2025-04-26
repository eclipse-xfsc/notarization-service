import dayjs from 'dayjs';
import { readFile, writeFile } from 'fs/promises';
import openapiTS from 'openapi-typescript';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

(async () => {
  const __dirname = dirname(fileURLToPath(import.meta.url));

  const schemaPath = resolve(__dirname, '../src/lib/acapy/types/openapi.json');
  const outputFilePath = resolve(__dirname, '../src/lib/acapy/types/acapy.ts');
  const prettierConfigPath = resolve(__dirname, '../prettierrc.js');

  const schema = await readFile(schemaPath, 'utf8');

  const types = await openapiTS(JSON.parse(schema), {
    formatter(node) {
      if (typeof node.example === 'string' && dayjs(node.example).isValid()) {
        return 'Date';
      }
    },
    prettierConfig: prettierConfigPath,
    immutableTypes: true,
  });

  await writeFile(outputFilePath, types, { encoding: 'utf-8' });
})();
