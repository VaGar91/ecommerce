import { useState } from "react";
import { useForm } from "react-hook-form";

import { ApiError } from "../../../shared/api/http";
import type { Product, ProductDraft } from "../../../shared/api/products";

type ProductFormValues = {
  name: string;
  sku: string;
  description: string;
  category: string;
  price: string;
  stock: string;
  weightKg: string;
};

type ProductFormProps = {
  product?: Product;
  onCancel: () => void;
  onSubmit: (product: ProductDraft) => Promise<void>;
};

const fieldNames = new Set<keyof ProductFormValues>([
  "name",
  "sku",
  "description",
  "category",
  "price",
  "stock",
  "weightKg",
]);

export function ProductForm({ product, onCancel, onSubmit }: ProductFormProps) {
  const [serverError, setServerError] = useState<string>();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormValues>({ defaultValues: toFormValues(product) });

  const submit = handleSubmit(async (values) => {
    setServerError(undefined);

    try {
      await onSubmit({
        name: values.name.trim(),
        sku: values.sku.trim(),
        description: values.description.trim(),
        category: values.category.trim(),
        price: Number(values.price),
        stock: Number(values.stock),
        weightKg: Number(values.weightKg),
      });
    } catch (error) {
      if (error instanceof ApiError) {
        const fieldErrors = error.problem.errors ?? {};
        let hasFieldError = false;

        Object.entries(fieldErrors).forEach(([field, message]) => {
          if (isProductField(field)) {
            setError(field, { type: "server", message });
            hasFieldError = true;
          }
        });

        if (!hasFieldError) {
          setServerError(error.message);
        }
        return;
      }

      setServerError("The product could not be saved. Please try again.");
    }
  });

  return (
    <form className="product-form" onSubmit={submit} noValidate>
      {serverError && <div className="alert alert-error" role="alert">{serverError}</div>}

      <div className="form-grid">
        <label className="field field-wide">
          <span>Name</span>
          <input
            aria-invalid={Boolean(errors.name)}
            {...register("name", {
              required: "Name is required",
              validate: (value) => value.trim().length > 0 || "Name is required",
              maxLength: { value: 200, message: "Name must not exceed 200 characters" },
            })}
          />
          <FieldError message={errors.name?.message} />
        </label>

        <label className="field">
          <span>SKU</span>
          <input
            aria-invalid={Boolean(errors.sku)}
            autoCapitalize="characters"
            {...register("sku", {
              required: "SKU is required",
              validate: (value) => value.trim().length > 0 || "SKU is required",
              maxLength: { value: 64, message: "SKU must not exceed 64 characters" },
            })}
          />
          <FieldError message={errors.sku?.message} />
        </label>

        <label className="field">
          <span>Category</span>
          <input
            aria-invalid={Boolean(errors.category)}
            {...register("category", {
              required: "Category is required",
              validate: (value) => value.trim().length > 0 || "Category is required",
              maxLength: { value: 100, message: "Category must not exceed 100 characters" },
            })}
          />
          <FieldError message={errors.category?.message} />
        </label>

        <label className="field field-wide">
          <span>Description</span>
          <textarea
            aria-invalid={Boolean(errors.description)}
            rows={4}
            {...register("description", {
              required: "Description is required",
              validate: (value) => value.trim().length > 0 || "Description is required",
              maxLength: { value: 2_000, message: "Description must not exceed 2,000 characters" },
            })}
          />
          <FieldError message={errors.description?.message} />
        </label>

        <label className="field">
          <span>Price</span>
          <input
            aria-invalid={Boolean(errors.price)}
            inputMode="decimal"
            min="0"
            step="0.01"
            type="number"
            {...register("price", {
              required: "Price is required",
              pattern: {
                value: /^\d{1,10}(\.\d{1,2})?$/,
                message: "Use up to 10 whole and 2 decimal digits",
              },
            })}
          />
          <FieldError message={errors.price?.message} />
        </label>

        <label className="field">
          <span>Stock</span>
          <input
            aria-invalid={Boolean(errors.stock)}
            inputMode="numeric"
            min="0"
            step="1"
            type="number"
            {...register("stock", {
              required: "Stock is required",
              pattern: { value: /^\d+$/, message: "Stock must be a whole number" },
              max: { value: 2_147_483_647, message: "Stock is too large" },
            })}
          />
          <FieldError message={errors.stock?.message} />
        </label>

        <label className="field">
          <span>Weight (kg)</span>
          <input
            aria-invalid={Boolean(errors.weightKg)}
            inputMode="decimal"
            min="0.001"
            step="0.001"
            type="number"
            {...register("weightKg", {
              required: "Weight is required",
              pattern: {
                value: /^\d{1,7}(\.\d{1,3})?$/,
                message: "Use up to 7 whole and 3 decimal digits",
              },
              validate: (value) => Number(value) > 0 || "Weight must be greater than zero",
            })}
          />
          <FieldError message={errors.weightKg?.message} />
        </label>
      </div>

      <div className="form-actions">
        <button className="button button-secondary" type="button" onClick={onCancel} disabled={isSubmitting}>
          Cancel
        </button>
        <button className="button button-primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving…" : product ? "Save changes" : "Create product"}
        </button>
      </div>
    </form>
  );
}

function FieldError({ message }: { message?: string }) {
  return message ? <small className="field-error">{message}</small> : null;
}

function toFormValues(product?: Product): ProductFormValues {
  return {
    name: product?.name ?? "",
    sku: product?.sku ?? "",
    description: product?.description ?? "",
    category: product?.category ?? "",
    price: product ? String(product.price) : "",
    stock: product ? String(product.stock) : "",
    weightKg: product ? String(product.weightKg) : "",
  };
}

function isProductField(field: string): field is keyof ProductFormValues {
  return fieldNames.has(field as keyof ProductFormValues);
}
