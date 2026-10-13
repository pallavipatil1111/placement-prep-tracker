import { expect, test } from "@playwright/test";
import { loginDemo } from "./helpers";

test("dashboard, board and revision queue work on a phone", async ({ page }) => {
  await loginDemo(page);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  await expect(page.locator("#heatmap .cell").first()).toBeVisible();

  await page.getByRole("button", { name: "Applications" }).click();
  await expect(page.getByRole("region", { name: "Wishlist" })).toBeVisible();
  const board = page.locator("#board");
  expect(await board.evaluate((el) => el.scrollWidth > el.clientWidth)).toBe(true);

  await page.getByRole("button", { name: /Problems/ }).click();
  await expect(page.locator(".due-item").first()).toBeVisible();
});
