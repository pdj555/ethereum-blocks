import type { Dataset } from "./types";

export {
  createDatasetFromCsv,
  DatasetImportError,
  MAX_BLOCK_RECORDS,
  MAX_CSV_COLUMNS,
  MAX_GAS_LIMIT,
  MAX_GAS_PRICE_WEI,
  MAX_TRANSACTION_RECORDS
} from "./dataset-import-core";
import { DatasetImportError } from "./dataset-import-core";

export const MAX_IMPORT_FILE_BYTES = 25 * 1024 * 1024;

export type DatasetSource =
  | { kind: "sample"; label: "Bundled 100-block sample" }
  | { kind: "local"; label: string };

type WorkerResponse =
  | { ok: true; dataset: Dataset }
  | { ok: false; message: string };

export async function loadDatasetFiles(
  blocksFile: File,
  transactionsFile: File
): Promise<Dataset> {
  assertFileSize(blocksFile);
  assertFileSize(transactionsFile);
  const [blocksBuffer, transactionsBuffer] = await Promise.all([
    blocksFile.arrayBuffer(),
    transactionsFile.arrayBuffer()
  ]);

  return new Promise<Dataset>((resolve, reject) => {
    const worker = new Worker(new URL("../workers/dataset-import-worker.ts", import.meta.url), {
      type: "module"
    });
    worker.onmessage = (event: MessageEvent<WorkerResponse>) => {
      worker.terminate();
      if (event.data.ok) {
        resolve(event.data.dataset);
        return;
      }
      reject(new DatasetImportError(event.data.message));
    };
    worker.onerror = () => {
      worker.terminate();
      reject(
        new DatasetImportError(
          "The CSV files could not be analyzed in this browser. Refresh and try again."
        )
      );
    };
    worker.postMessage(
      { blocksBuffer, transactionsBuffer },
      [blocksBuffer, transactionsBuffer]
    );
  });
}

function assertFileSize(file: File) {
  if (file.size > MAX_IMPORT_FILE_BYTES) {
    throw new DatasetImportError(
      `${file.name} is larger than 25 MiB. Export a smaller slice and try again.`
    );
  }
}
