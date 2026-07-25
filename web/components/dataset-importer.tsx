"use client";

import { useRef, useState } from "react";
import { DatasetImportError, type DatasetSource } from "@/lib/dataset-import";

type DatasetImporterProps = {
  source: DatasetSource;
  onLoad: (blocksFile: File, transactionsFile: File) => Promise<void>;
  onReset: () => void;
};

export function DatasetImporter({ source, onLoad, onReset }: DatasetImporterProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function closeDialog() {
    if (submitting) {
      return;
    }
    dialogRef.current?.close();
  }

  return (
    <>
      <div className="dataset-source" aria-label="Dataset source">
        <div className="dataset-source__meta">
          <span>Dataset</span>
          <strong>{source.label}</strong>
          <span>Local parsing · files never uploaded</span>
        </div>
        <div className="dataset-source__actions">
          {source.kind === "local" ? (
            <button type="button" onClick={onReset}>
              Use bundled sample
            </button>
          ) : null}
          <button
            type="button"
            className="dataset-source__primary"
            onClick={() => dialogRef.current?.showModal()}
          >
            {source.kind === "sample" ? "Load your CSVs" : "Replace CSVs"}
          </button>
        </div>
      </div>

      <dialog
        ref={dialogRef}
        className="dataset-dialog"
        aria-labelledby="dataset-dialog-title"
        aria-busy={submitting}
        onCancel={(event) => {
          if (submitting) {
            event.preventDefault();
          }
        }}
        onClose={() => {
          formRef.current?.reset();
          setError(null);
        }}
      >
        <form
          ref={formRef}
          onSubmit={async (event) => {
            event.preventDefault();
            setError(null);
            const formData = new FormData(event.currentTarget);
            const blocksFile = formData.get("blocks");
            const transactionsFile = formData.get("transactions");

            if (
              !(blocksFile instanceof File) ||
              !blocksFile.name ||
              !(transactionsFile instanceof File) ||
              !transactionsFile.name
            ) {
              setError("Choose both CSV files, then try again.");
              return;
            }

            setSubmitting(true);
            try {
              await onLoad(blocksFile, transactionsFile);
              closeDialog();
              formRef.current?.reset();
            } catch (loadError) {
              setError(
                loadError instanceof DatasetImportError
                  ? loadError.message
                  : "The CSV files could not be analyzed. Check the exports and try again."
              );
            } finally {
              setSubmitting(false);
            }
          }}
        >
          <div className="dataset-dialog__header">
            <div>
              <p>Local dataset</p>
              <h2 id="dataset-dialog-title">Analyze your own data</h2>
            </div>
            <p>Choose exports that match the bundled block and transaction CSV layouts.</p>
          </div>

          <div className="dataset-dialog__fields">
            <label className="dataset-dialog__field">
              <span>Blocks CSV</span>
              <input name="blocks" type="file" accept=".csv,text/csv" />
            </label>
            <label className="dataset-dialog__field">
              <span>Transactions CSV</span>
              <input name="transactions" type="file" accept=".csv,text/csv" />
            </label>
          </div>

          <p className="dataset-dialog__hint">
            25 MiB per file · processed only in this browser · schema examples: {" "}
            <a href="/ethereumP1data.csv">blocks</a> + {" "}
            <a href="/ethereumtransactions1.csv">transactions</a>
          </p>
          {error ? (
            <p className="dataset-dialog__error" role="alert">
              {error}
            </p>
          ) : null}

          <div className="dataset-dialog__actions">
            <button type="button" onClick={closeDialog} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="dataset-source__primary" disabled={submitting}>
              {submitting ? "Analyzing…" : "Analyze locally"}
            </button>
          </div>
        </form>
      </dialog>
    </>
  );
}
