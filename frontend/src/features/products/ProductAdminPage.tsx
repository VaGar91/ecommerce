import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";

import { ApiError } from "../../shared/api/http";
import {
  createProduct,
  deleteProduct,
  listProducts,
  updateProduct,
  type Product,
  type ProductDraft,
} from "./api/products";
import { ProductForm } from "./components/ProductForm";

const PAGE_SIZE = 10;

type Filters = {
  query: string;
  category: string;
};

type Editor =
  | { mode: "create" }
  | { mode: "edit"; product: Product };

export function ProductAdminPage() {
  const queryClient = useQueryClient();
  const [draftFilters, setDraftFilters] = useState<Filters>({ query: "", category: "" });
  const [filters, setFilters] = useState<Filters>(draftFilters);
  const [page, setPage] = useState(0);
  const [editor, setEditor] = useState<Editor>();
  const [deleteTarget, setDeleteTarget] = useState<Product>();
  const [feedback, setFeedback] = useState<string>();

  const productsQuery = useQuery({
    queryKey: ["products", "admin", filters, page],
    queryFn: () => listProducts({ ...filters, page, size: PAGE_SIZE }),
    placeholderData: (previousData) => previousData,
  });

  const createMutation = useMutation({ mutationFn: createProduct });
  const updateMutation = useMutation({
    mutationFn: ({ productId, product }: { productId: string; product: ProductDraft }) =>
      updateProduct(productId, product),
  });
  const deleteMutation = useMutation({ mutationFn: deleteProduct });

  const applyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFilters({ query: draftFilters.query.trim(), category: draftFilters.category.trim() });
    setPage(0);
  };

  const saveProduct = async (product: ProductDraft) => {
    if (editor?.mode === "edit") {
      await updateMutation.mutateAsync({ productId: editor.product.id, product });
      setFeedback("Product updated.");
    } else {
      await createMutation.mutateAsync(product);
      setFeedback("Product created.");
    }

    setEditor(undefined);
    void queryClient.invalidateQueries({ queryKey: ["products"] });
  };

  const confirmDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    try {
      await deleteMutation.mutateAsync(deleteTarget.id);
      setDeleteTarget(undefined);
      setFeedback("Product deleted.");

      if (productsQuery.data?.content.length === 1 && page > 0) {
        setPage((currentPage) => currentPage - 1);
      } else {
        void queryClient.invalidateQueries({ queryKey: ["products"] });
      }
    } catch {
      // The mutation error is rendered in the confirmation dialog.
    }
  };

  const openCreate = () => {
    setFeedback(undefined);
    createMutation.reset();
    setEditor({ mode: "create" });
  };

  const openEdit = (product: Product) => {
    setFeedback(undefined);
    updateMutation.reset();
    setEditor({ mode: "edit", product });
  };

  const openDelete = (product: Product) => {
    setFeedback(undefined);
    deleteMutation.reset();
    setDeleteTarget(product);
  };

  const products = productsQuery.data?.content ?? [];
  const totalPages = productsQuery.data?.totalPages ?? 0;

  return (
    <section className="admin-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>Manage products</h1>
          <p className="lede">Create, update, find and retire catalog inventory from one workspace.</p>
        </div>
        <button className="button button-primary" type="button" onClick={openCreate}>
          Add product
        </button>
      </div>

      {feedback && <div className="alert alert-success" role="status">{feedback}</div>}

      <form className="filter-bar" onSubmit={applyFilters} role="search">
        <label className="field filter-query">
          <span>Search</span>
          <input
            type="search"
            value={draftFilters.query}
            onChange={(event) => setDraftFilters((current) => ({ ...current, query: event.target.value }))}
            placeholder="Name, SKU or description"
            maxLength={200}
          />
        </label>
        <label className="field">
          <span>Category</span>
          <input
            value={draftFilters.category}
            onChange={(event) => setDraftFilters((current) => ({ ...current, category: event.target.value }))}
            placeholder="All categories"
            maxLength={100}
          />
        </label>
        <button className="button button-secondary" type="submit">Apply filters</button>
      </form>

      <div className="table-panel" aria-busy={productsQuery.isFetching}>
        {productsQuery.isPending && <LoadingState />}
        {productsQuery.isError && <ErrorState error={productsQuery.error} onRetry={() => productsQuery.refetch()} />}
        {productsQuery.isSuccess && products.length === 0 && <EmptyState filtered={Boolean(filters.query || filters.category)} />}
        {productsQuery.isSuccess && products.length > 0 && (
          <>
            <div className="table-scroll">
              <table className="product-table">
                <caption className="visually-hidden">Products in the catalog</caption>
                <thead>
                  <tr>
                    <th scope="col">Product</th>
                    <th scope="col">Category</th>
                    <th scope="col" className="numeric">Price</th>
                    <th scope="col" className="numeric">Stock</th>
                    <th scope="col" className="numeric">Weight</th>
                    <th scope="col"><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr key={product.id}>
                      <td>
                        <strong>{product.name}</strong>
                        <small>{product.sku}</small>
                      </td>
                      <td>{product.category}</td>
                      <td className="numeric">{formatDecimal(product.price, 2)}</td>
                      <td className="numeric">{product.stock.toLocaleString()}</td>
                      <td className="numeric">{formatDecimal(product.weightKg, 3)} kg</td>
                      <td>
                        <div className="row-actions">
                          <button
                            className="text-button"
                            type="button"
                            onClick={() => openEdit(product)}
                            aria-label={`Edit ${product.name}`}
                          >
                            Edit
                          </button>
                          <button
                            className="text-button text-button-danger"
                            type="button"
                            onClick={() => openDelete(product)}
                            aria-label={`Delete ${product.name}`}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="pagination" aria-label="Product pagination">
              <p>
                Page {page + 1} of {totalPages} · {productsQuery.data.totalElements.toLocaleString()} products
              </p>
              <div>
                <button
                  className="button button-secondary button-small"
                  type="button"
                  onClick={() => setPage((currentPage) => currentPage - 1)}
                  disabled={page === 0 || productsQuery.isFetching}
                >
                  Previous
                </button>
                <button
                  className="button button-secondary button-small"
                  type="button"
                  onClick={() => setPage((currentPage) => currentPage + 1)}
                  disabled={page + 1 >= totalPages || productsQuery.isFetching}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {editor && (
        <div className="dialog-backdrop" role="presentation">
          <section className="dialog-panel dialog-panel-wide" role="dialog" aria-modal="true" aria-labelledby="product-form-title">
            <div className="dialog-heading">
              <div>
                <p className="eyebrow">Catalog record</p>
                <h2 id="product-form-title">{editor.mode === "edit" ? "Edit product" : "Create product"}</h2>
              </div>
              <button className="close-button" type="button" onClick={() => setEditor(undefined)} aria-label="Close product form">
                ×
              </button>
            </div>
            <ProductForm
              key={editor.mode === "edit" ? editor.product.id : "new-product"}
              product={editor.mode === "edit" ? editor.product : undefined}
              onCancel={() => setEditor(undefined)}
              onSubmit={saveProduct}
            />
          </section>
        </div>
      )}

      {deleteTarget && (
        <div className="dialog-backdrop" role="presentation">
          <section className="dialog-panel" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description">
            <p className="eyebrow">Permanent action</p>
            <h2 id="delete-title">Delete product?</h2>
            <p id="delete-description">
              <strong>{deleteTarget.name}</strong> ({deleteTarget.sku}) will be removed from the catalog.
            </p>
            {deleteMutation.isError && (
              <div className="alert alert-error" role="alert">{getErrorMessage(deleteMutation.error)}</div>
            )}
            <div className="form-actions">
              <button
                className="button button-secondary"
                type="button"
                onClick={() => setDeleteTarget(undefined)}
                disabled={deleteMutation.isPending}
              >
                Cancel
              </button>
              <button
                className="button button-danger"
                type="button"
                onClick={confirmDelete}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? "Deleting…" : "Delete product"}
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

function LoadingState() {
  return <div className="panel-state" role="status">Loading products…</div>;
}

function ErrorState({ error, onRetry }: { error: Error; onRetry: () => void }) {
  return (
    <div className="panel-state" role="alert">
      <strong>Products could not be loaded.</strong>
      <p>{getErrorMessage(error)}</p>
      <button className="button button-secondary button-small" type="button" onClick={onRetry}>Try again</button>
    </div>
  );
}

function EmptyState({ filtered }: { filtered: boolean }) {
  return (
    <div className="panel-state">
      <strong>{filtered ? "No products match these filters." : "No products yet."}</strong>
      <p>{filtered ? "Adjust the search or category and try again." : "Add the first product to start the catalog."}</p>
    </div>
  );
}

function getErrorMessage(error: Error): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "The server could not be reached. Please try again.";
}

function formatDecimal(value: number, maximumFractionDigits: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits,
  }).format(value);
}
