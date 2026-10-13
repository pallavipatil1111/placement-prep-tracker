import { expect, test } from "@playwright/test";
import { DEMO, loginDemo, registerFreshUser } from "./helpers";

test("wrong password shows an error and stays on sign-in", async ({ page }) => {
  await page.goto("/");
  const form = page.locator("#login-form");
  await form.getByLabel("Email", { exact: true }).fill(DEMO.email);
  await form.getByLabel("Password", { exact: true }).fill("not-the-password");
  await form.getByRole("button", { name: "Sign in" }).click();
  await expect(form.getByRole("alert")).toHaveText("Invalid email or password");
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeHidden();
});

test("demo account opens a populated dashboard", async ({ page }) => {
  await loginDemo(page);
  const streak = page.locator('[data-k="currentStreak"]');
  await expect(streak).not.toHaveText("0");
  expect(Number(await page.locator('#view-dashboard [data-k="totalSolved"]').textContent())).toBeGreaterThan(50);
  expect(await page.locator("#heatmap .cell:not(.future)").count()).toBeGreaterThan(50);
  expect(await page.locator("#heatmap .cell.l4").count()).toBeGreaterThan(0);
  await expect(page.locator("#funnel .funnel-row")).toHaveCount(4);
  await expect(page.locator("#upcoming li").first()).toBeVisible();
  await expect(page.locator("#due-badge")).toBeVisible();
});

test("session survives a reload and logging out ends it", async ({ page }) => {
  await loginDemo(page);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Dashboard" })).toBeVisible();
  await page.getByRole("button", { name: "Log out" }).click();
  await expect(page.getByRole("button", { name: "Try the demo account" })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("button", { name: "Try the demo account" })).toBeVisible();
});

test("an invalid stored token falls back to sign-in", async ({ page }) => {
  await page.goto("/");
  await page.evaluate(() => localStorage.setItem("prep.token", "garbage"));
  await page.reload();
  await expect(page.getByRole("button", { name: "Try the demo account" })).toBeVisible();
});

test("registration validates fields and new users start empty", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("tab", { name: "Create account" }).click();
  const form = page.locator("#register-form");
  await form.getByLabel("Name", { exact: true }).fill("Short");
  await form.getByLabel("Email", { exact: true }).fill("short@example.com");
  await form.getByLabel("Password", { exact: true }).fill("123");
  await form.getByRole("button", { name: "Create account" }).click();
  await expect(form.getByRole("alert")).toContainText("password");
  await expect(form.getByLabel("Password", { exact: true })).toHaveAttribute("aria-invalid", "true");

  await registerFreshUser(page, "New Student");
  await expect(page.locator('[data-k="currentStreak"]')).toHaveText("0");
  await expect(page.locator("#topics")).toContainText("Log solved problems");
  await expect(page.locator("#upcoming")).toContainText("Nothing scheduled");
});
