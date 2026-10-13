import { expect, type Page } from "@playwright/test";

export const DEMO = { email: "demo@prep.in", password: "demo1234" };

/** Creates a brand-new account so tests that change data never interfere with each other. */
export async function registerFreshUser(page: Page, name = "Test User"): Promise<string> {
  const email = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`;
  await page.goto("/");
  await page.getByRole("tab", { name: "Create account" }).click();
  const form = page.locator("#register-form");
  await form.getByLabel("Name", { exact: true }).fill(name);
  await form.getByLabel("Email", { exact: true }).fill(email);
  await form.getByLabel("Password", { exact: true }).fill("secret123");
  await form.getByRole("button", { name: "Create account" }).click();
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  return email;
}

export async function loginDemo(page: Page) {
  await page.goto("/");
  await page.getByRole("button", { name: "Try the demo account" }).click();
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
}

export function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
