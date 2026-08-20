import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRef, useState, type ChangeEvent, type DragEvent, type FormEvent } from "react";

import { ApiError } from "../../shared/api/http";
import { importProducts, type ProductImportReport } from "./api/productImports";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const REQUIRED_HEADERS = ["name", "sku", "description", "category", "price", "stock", "weight_kg"];

export function ProductImportPage() {
  const queryClient = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File>();
  const [fileError, setFileError] = useState<string>();
  const importMutation = useMutation({
    mutationFn: importProducts,
    onSuccess: (result) => {
      if (result.created + result.updated > 0) {
        void queryClient.invalidateQueries({ queryKey: ["products"] });
        void queryClient.invalidateQueries({ queryKey: ["product-categories"] });
      }
    },
  });

  const chooseFile = (nextFile?: File) => {
    importMutation.reset();

    if (!nextFile) {
      setFile(undefined);
      setFileError(undefined);
      return;
    }

    const error = validateFile(nextFile);
    setFile(error ? undefined : nextFile);
    setFileError(error);
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    chooseFile(event.target.files?.[0]);
  };

  const handleDrop = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    chooseFile(event.dataTransfer.files[0]);
  };

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (file) {
      importMutation.mutate(file);
    }
  };

  const clearFile = () => {
    chooseFile(undefined);
    if (inputRef.current) {
      inputRef.current.value = "";
    }
  };

  const result = importMutation.data;

  return (
    <section className="import-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Bulk operations</p>
          <h1>Import product data</h1>
          <p className="lede">Import valid catalog records and receive row-level feedback for rejected data.</p>
        </div>
      </div>

      <div className="import-layout">
        <form className="import-card" onSubmit={submit}>
          <div>
            <h2>Choose a CSV file</h2>
            <p>Every row is validated; invalid rows are skipped without blocking valid products.</p>
          </div>

          <label
            className="import-dropzone"
            htmlFor="product-csv"
            onDragOver={(event) => event.preventDefault()}
            onDrop={handleDrop}
          >
            <input
              ref={inputRef}
              id="product-csv"
              className="visually-hidden"
              type="file"
              aria-label="CSV file"
              accept=".csv,text/csv"
              aria-describedby="csv-constraints"
              onChange={handleFileChange}
            />
            <span className="upload-mark" aria-hidden="true">CSV</span>
            <strong>Drop a file here or browse</strong>
            <small id="csv-constraints">UTF-8 CSV · maximum 5 MB · maximum 10,000 rows</small>
          </label>

          {fileError && <div className="alert alert-error" role="alert">{fileError}</div>}

          {file && (
            <div className="selected-file">
              <div>
                <strong>{file.name}</strong>
                <small>{formatFileSize(file.size)}</small>
              </div>
              <button className="text-button text-button-danger" type="button" onClick={clearFile}>
                Remove
              </button>
            </div>
          )}

          {importMutation.isError && (
            <div className="alert alert-error" role="alert">{getErrorMessage(importMutation.error)}</div>
          )}

          <button
            className="button button-primary import-submit"
            type="submit"
            disabled={!file || importMutation.isPending}
          >
            {importMutation.isPending ? "Importing…" : "Import products"}
          </button>
        </form>

        <aside className="format-card" aria-labelledby="format-title">
          <p className="eyebrow">Required format</p>
          <h2 id="format-title">Seven exact headers</h2>
          <div className="header-list" aria-label="Required CSV headers">
            {REQUIRED_HEADERS.map((header) => <code key={header}>{header}</code>)}
          </div>
          <ul>
            <li>SKUs are matched case-insensitively and stored in uppercase.</li>
            <li>Existing SKUs are updated; new SKUs are created.</li>
            <li>HTML-like markup and invalid values reject only the affected rows.</li>
          </ul>
        </aside>
      </div>

      {result && <ImportReport report={result} />}
    </section>
  );
}

function ImportReport({ report }: { report: ProductImportReport }) {
  if (report.errors.length > 0) {
    return <ImportWithRejectedRows report={report} />;
  }

  return (
    <section className="report-panel report-success" aria-labelledby="import-success-title" role="status">
      <div>
        <p className="eyebrow">All rows accepted</p>
        <h2 id="import-success-title">Import complete</h2>
        <p>All {report.totalRows.toLocaleString()} rows passed validation and are now in the catalog.</p>
      </div>
      <ReportMetrics report={report} />
    </section>
  );
}

function ImportWithRejectedRows({ report }: { report: ProductImportReport }) {
  const imported = report.created + report.updated;
  const title = imported > 0 ? "Import completed with rejected rows" : "No rows imported";

  return (
    <section className="report-panel report-partial" aria-labelledby="import-report-title" role="status">
      <div className="report-heading">
        <div>
          <p className="eyebrow">{imported > 0 ? "Valid rows committed" : "Every row rejected"}</p>
          <h2 id="import-report-title">{title}</h2>
          <p>
            {imported > 0
              ? `${imported.toLocaleString()} ${imported === 1 ? "row was" : "rows were"} imported. Correct the rejected rows and upload them again.`
              : "Correct the issues below and upload the affected rows again."}
          </p>
        </div>
        <ReportMetrics report={report} />
      </div>

      <div className="table-scroll">
        <table className="error-table">
          <caption>{report.errors.length.toLocaleString()} validation issues</caption>
          <thead>
            <tr>
              <th scope="col">Row</th>
              <th scope="col">Field</th>
              <th scope="col">Issue</th>
            </tr>
          </thead>
          <tbody>
            {report.errors.map((error, index) => (
              <tr key={`${error.row}-${error.field}-${index}`}>
                <td>{error.row}</td>
                <td><code>{error.field}</code></td>
                <td>{error.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function ReportMetrics({ report }: { report: ProductImportReport }) {
  return (
    <dl className="report-metrics">
      <div><dt>Rows</dt><dd>{report.totalRows.toLocaleString()}</dd></div>
      <div><dt>Created</dt><dd>{report.created.toLocaleString()}</dd></div>
      <div><dt>Updated</dt><dd>{report.updated.toLocaleString()}</dd></div>
      <div><dt>Rejected</dt><dd>{report.rejected.toLocaleString()}</dd></div>
    </dl>
  );
}

function validateFile(file: File): string | undefined {
  if (!file.name.toLowerCase().endsWith(".csv")) {
    return "Choose a file with a .csv extension.";
  }
  if (file.size === 0) {
    return "The selected CSV file is empty.";
  }
  if (file.size > MAX_FILE_SIZE) {
    return "The selected CSV file exceeds the 5 MB limit.";
  }
  return undefined;
}

function getErrorMessage(error: Error): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "The import could not be completed. Please try again.";
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} bytes`;
  }
  return `${(bytes / 1024).toFixed(1)} KB`;
}
