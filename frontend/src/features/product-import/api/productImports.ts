import { ApiError, type ApiProblem } from "../../../shared/api/http";

export type ProductImportRowError = {
  row: number;
  field: string;
  message: string;
};

export type ProductImportReport = {
  totalRows: number;
  created: number;
  updated: number;
  errors: ProductImportRowError[];
};

export type ProductImportResult =
  | { outcome: "imported"; report: ProductImportReport }
  | { outcome: "rejected"; report: ProductImportReport };

export async function importProducts(file: File): Promise<ProductImportResult> {
  const body = new FormData();
  body.append("file", file);

  const response = await fetch("/api/product-imports", {
    method: "POST",
    headers: { Accept: "application/json" },
    body,
  });
  const responseBody: unknown = await readJson(response);

  if (response.status === 422) {
    if (isImportReport(responseBody)) {
      return { outcome: "rejected", report: responseBody };
    }
    throw new ApiError(422, unreadableResponseProblem(422));
  }

  if (!response.ok) {
    throw new ApiError(response.status, isApiProblem(responseBody)
      ? responseBody
      : unreadableResponseProblem(response.status));
  }

  if (!isImportReport(responseBody)) {
    throw new ApiError(response.status, unreadableResponseProblem(response.status));
  }

  return { outcome: "imported", report: responseBody };
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json() as unknown;
  } catch {
    return undefined;
  }
}

function isImportReport(value: unknown): value is ProductImportReport {
  if (!isRecord(value)) {
    return false;
  }

  return isNonNegativeInteger(value.totalRows)
    && isNonNegativeInteger(value.created)
    && isNonNegativeInteger(value.updated)
    && Array.isArray(value.errors)
    && value.errors.every(isRowError);
}

function isRowError(value: unknown): value is ProductImportRowError {
  return isRecord(value)
    && isNonNegativeInteger(value.row)
    && typeof value.field === "string"
    && typeof value.message === "string";
}

function isApiProblem(value: unknown): value is ApiProblem {
  return isRecord(value)
    && (value.title === undefined || typeof value.title === "string")
    && (value.status === undefined || typeof value.status === "number")
    && (value.detail === undefined || typeof value.detail === "string");
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isNonNegativeInteger(value: unknown): value is number {
  return typeof value === "number" && Number.isInteger(value) && value >= 0;
}

function unreadableResponseProblem(status: number): ApiProblem {
  return {
    title: "Import failed",
    status,
    detail: "The server returned an unreadable import response",
  };
}
