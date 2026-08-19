import { apiRequest } from "../../../shared/api/http";

export type PurchaseItem = {
  productId: string;
  quantity: number;
};

export type PurchaseRequest = {
  items: PurchaseItem[];
  paymentToken: string;
};

export type OrderItem = {
  productId: string;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
};

export type Order = {
  id: string;
  status: "PAID";
  items: OrderItem[];
  total: number;
  currency: string;
  paymentReference: string;
  createdAt: string;
};

export function purchaseOrder(request: PurchaseRequest): Promise<Order> {
  return apiRequest<Order>("/orders", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
