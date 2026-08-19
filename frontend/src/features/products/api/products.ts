import { apiRequest } from "../../../shared/api/http";

export type Product = {
  id: string;
  name: string;
  sku: string;
  description: string;
  category: string;
  price: number;
  stock: number;
  weightKg: number;
};

export type ProductDraft = Omit<Product, "id">;

export type ProductPage = {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductSearch = {
  query: string;
  category: string;
  page: number;
  size: number;
};

export function listProducts(search: ProductSearch): Promise<ProductPage> {
  const parameters = new URLSearchParams({
    page: String(search.page),
    size: String(search.size),
  });

  if (search.query.trim()) {
    parameters.set("query", search.query.trim());
  }
  if (search.category.trim()) {
    parameters.set("category", search.category.trim());
  }

  return apiRequest<ProductPage>(`/products?${parameters.toString()}`);
}

export function createProduct(product: ProductDraft): Promise<Product> {
  return apiRequest<Product>("/products", {
    method: "POST",
    body: JSON.stringify(product),
  });
}

export function updateProduct(productId: string, product: ProductDraft): Promise<Product> {
  return apiRequest<Product>(`/products/${productId}`, {
    method: "PUT",
    body: JSON.stringify(product),
  });
}

export function deleteProduct(productId: string): Promise<void> {
  return apiRequest<void>(`/products/${productId}`, { method: "DELETE" });
}
