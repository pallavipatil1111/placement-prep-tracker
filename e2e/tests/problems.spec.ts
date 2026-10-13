import { expect, test } from "@playwright/test";
import { isoDaysAgo, registerFreshUser } from "./helpers";

async function logProblem(page: import("@playwright/test").Page, title: string, topic: string, solvedOn: string) {
  await page.getByRole("button", { name: "Problems" }).click();
  await page.getByRole("button", { name: "+ Log a problem" }).click();
  const dialog = page.locator("#problem-dialog");
  await dialog.getByLabel("Title", { exact: true }).fill(title);
  await dialog.getByLabel("Link", { exact: true }).fill("https://leetcode.com/problems/coin-change/");
  await dialog.locator("select[name=difficulty]").selectOption("MEDIUM");
  await dialog.locator("select[name=topic]").selectOption(topic);
  await dialog.getByLabel("Solved on", { exact: true }).fill(solvedOn);
  await dialog.getByRole("button", { name: "Save" }).click();
  await expect(dialog).toBeHidden();
}

test("a backdated problem is due, and grading it schedules the next revision", async ({ page }) => {
  await registerFreshUser(page);
  await logProblem(page, "Coin Change", "DP", isoDaysAgo(3));

  const item = page.locator(".due-item", { hasText: "Coin Change" });
  await expect(item).toContainText("2 days overdue");
  await expect(item.getByRole("button", { name: /Good/ })).toContainText("6d");
  await expect(page.locator("#due-badge")).toHaveText("1");

  await item.getByRole("button", { name: /Good/ }).click();
  await expect(page.locator(".toast").last()).toContainText("next revision in 6 days");
  await expect(page.locator("#due-list")).toContainText("All caught up");
  await expect(page.locator("#problem-table tbody tr")).toContainText(["in 6 days"]);
  await expect(page.locator("#due-badge")).toBeHidden();

  await page.getByRole("button", { name: "Dashboard" }).click();
  await expect(page.locator('#view-dashboard [data-k="todayCount"]')).toHaveText("1");
  await expect(page.locator('[data-k="currentStreak"]')).toHaveText("1");
});

test("today's problem is not due until tomorrow; filters and delete work", async ({ page }) => {
  await registerFreshUser(page);
  await logProblem(page, "Number of Islands", "GRAPHS", isoDaysAgo(0));
  await expect(page.locator(".toast").last()).toContainText("first revision tomorrow");
  await logProblem(page, "Two Sum", "HASHING", isoDaysAgo(0));

  await expect(page.locator("#due-list")).toContainText("All caught up");
  await expect(page.locator("#problem-table tbody tr")).toHaveCount(2);
  await page.locator("#filter-topic").selectOption("GRAPHS");
  await expect(page.locator("#problem-table tbody tr")).toHaveCount(1);
  await page.locator("#filter-topic").selectOption("");
  await page.getByLabel("Search problems", { exact: true }).fill("two");
  await expect(page.locator("#problem-table tbody tr")).toHaveCount(1);
  await page.getByLabel("Search problems", { exact: true }).fill("");
  await expect(page.locator("#problem-table tbody tr")).toHaveCount(2);

  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "Delete Two Sum" }).click();
  await expect(page.locator("#problem-table tbody tr")).toHaveCount(1);
});

test("future solved date is rejected", async ({ page }) => {
  await registerFreshUser(page);
  await page.getByRole("button", { name: "Problems" }).click();
  await page.getByRole("button", { name: "+ Log a problem" }).click();
  const dialog = page.locator("#problem-dialog");
  await dialog.getByLabel("Title", { exact: true }).fill("Time travel");
  await dialog.getByLabel("Solved on", { exact: true }).evaluate((el: HTMLInputElement) => { el.removeAttribute("max"); });
  await dialog.getByLabel("Solved on", { exact: true }).fill("2099-01-01");
  await dialog.getByRole("button", { name: "Save" }).click();
  await expect(dialog.getByRole("alert")).toContainText("future");
});

test("daily goal can be changed", async ({ page }) => {
  await registerFreshUser(page);
  await page.getByRole("button", { name: /Goal:/ }).click();
  await page.locator("#goal-dialog").getByLabel("Problems per day", { exact: true }).fill("5");
  await page.locator("#goal-dialog").getByRole("button", { name: "Save" }).click();
  await expect(page.locator("#goal-label")).toHaveText("5");
  await expect(page.locator('#view-dashboard [data-k="dailyGoal"]')).toHaveText("5");
});
