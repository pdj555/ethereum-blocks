import { createDatasetFromCsv } from "../lib/dataset-import-core";

type WorkerRequest = {
  blocksBuffer: ArrayBuffer;
  transactionsBuffer: ArrayBuffer;
};

self.onmessage = (event: MessageEvent<WorkerRequest>) => {
  try {
    const decoder = new TextDecoder();
    const blocksCsv = decoder.decode(event.data.blocksBuffer);
    const transactionsCsv = decoder.decode(event.data.transactionsBuffer);
    const dataset = createDatasetFromCsv(blocksCsv, transactionsCsv);
    self.postMessage({ ok: true, dataset });
  } catch (error) {
    self.postMessage({
      ok: false,
      message:
        error instanceof Error
          ? error.message
          : "The CSV files could not be analyzed. Check the exports and try again."
    });
  }
};

export {};
