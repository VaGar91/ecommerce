import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";

import { ProductImportPage } from "./ProductImportPage";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("ProductImportPage", () => {
  it("rejects a non-CSV file before uploading", () => {
    const fetchMock = vi.fn<typeof fetch>();
    vi.stubGlobal("fetch", fetchMock);
    renderImportPage();

    selectFile(new File(["not,csv"], "products.txt", { type: "text/plain" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Choose a file with a .csv extension.");
    expect(screen.getByRole("button", { name: "Import products" })).toBeDisabled();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("uploads a CSV as multipart data and renders the committed report", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      totalRows: 2,
      created: 1,
      updated: 1,
      rejected: 0,
      errors: [],
    }));
    vi.stubGlobal("fetch", fetchMock);
    renderImportPage();
    const file = validCsvFile();

    selectFile(file);
    expect(screen.getByText("products.csv")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Import products" }));

    const report = await screen.findByRole("status");
    expect(within(report).getByRole("heading", { name: "Import complete" })).toBeInTheDocument();
    expect(within(report).getByText("All 2 rows passed validation and are now in the catalog.")).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, request] = fetchMock.mock.calls[0];
    expect(request?.method).toBe("POST");
    expect(request?.body).toBeInstanceOf(FormData);
    expect((request?.body as FormData).get("file")).toBe(file);
    expect(new Headers(request?.headers).has("Content-Type")).toBe(false);
  });

  it("commits valid rows and renders errors for rejected rows", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      totalRows: 3,
      created: 1,
      updated: 0,
      rejected: 2,
      errors: [
        { row: 3, field: "price", message: "must be a decimal number" },
        { row: 4, field: "name", message: "must not contain HTML markup" },
      ],
    }));
    vi.stubGlobal("fetch", fetchMock);
    renderImportPage();

    selectFile(validCsvFile());
    fireEvent.click(screen.getByRole("button", { name: "Import products" }));

    expect(await screen.findByRole("heading", { name: "Import completed with rejected rows" })).toBeInTheDocument();
    expect(screen.getByText("Valid rows committed")).toBeInTheDocument();
    expect(screen.getByText(/1 row was imported/)).toBeInTheDocument();
    expect(screen.getByText("2 validation issues")).toBeInTheDocument();
    expect(screen.getByText("must be a decimal number")).toBeInTheDocument();
    expect(screen.getByText("must not contain HTML markup")).toBeInTheDocument();
  });

  it("shows the backend problem when the CSV document is malformed", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      title: "Invalid CSV file",
      status: 400,
      detail: "CSV headers must contain exactly: name, sku, description, category, price, stock, weight_kg",
    }, 400));
    vi.stubGlobal("fetch", fetchMock);
    renderImportPage();

    selectFile(validCsvFile());
    fireEvent.click(screen.getByRole("button", { name: "Import products" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("CSV headers must contain exactly");
    });
  });
});

function renderImportPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ProductImportPage />
    </QueryClientProvider>,
  );
}

function selectFile(file: File) {
  fireEvent.change(screen.getByLabelText("CSV file"), { target: { files: [file] } });
}

function validCsvFile(): File {
  return new File([
    "name,sku,description,category,price,stock,weight_kg\n",
    "Keyboard,KEY-001,Mechanical keyboard,Accessories,49.90,25,0.850\n",
  ], "products.csv", { type: "text/csv" });
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
