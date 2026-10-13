import { expect, test } from "@playwright/test";
import { registerFreshUser } from "./helpers";

test.beforeEach(async ({ page }) => {
  await registerFreshUser(page);
  await page.getByRole("button", { name: "Applications" }).click();
});

const column = (page: import("@playwright/test").Page, label: string) => page.getByRole("region", { name: label });

test("add an application, move it through the pipeline and see its history", async ({ page }) => {
  await page.getByRole("button", { name: "+ Add application" }).click();
  const dialog = page.locator("#application-dialog");
  await dialog.getByLabel("Company", { exact: true }).fill("Persistent Systems");
  await dialog.getByLabel("Role", { exact: true }).fill("Graduate Engineer Trainee");
  await dialog.getByLabel("Location", { exact: true }).fill("Pune");
  await dialog.locator("select[name=source]").selectOption("CAMPUS");
  await dialog.getByLabel("Package (LPA)", { exact: true }).fill("6.5");
  await dialog.getByLabel("Next step", { exact: true }).fill("Technical round");
  await dialog.getByLabel("On", { exact: true }).fill("2099-01-15");
  await dialog.getByRole("button", { name: "Save" }).click();
  await expect(dialog).toBeHidden();

  const applied = column(page, "Applied");
  const card = applied.locator(".app-card", { hasText: "Persistent Systems" });
  await expect(card).toContainText("₹6.5 LPA");
  await expect(card).toContainText("Technical round");

  await card.getByLabel("Move Persistent Systems to stage", { exact: true }).selectOption("OA");
  await expect(column(page, "Online test").locator(".app-card")).toHaveCount(1);
  await expect(page.locator(".toast").last()).toContainText("moved to Online test");

  await column(page, "Online test").locator(".app-card").dragTo(column(page, "Interview"));
  await expect(column(page, "Interview").locator(".app-card")).toHaveCount(1);

  await column(page, "Interview").getByRole("button", { name: "Details" }).click();
  await expect(dialog.locator("#history li")).toHaveCount(3);
  await expect(dialog.locator("#history")).toContainText("Online test → Interview");
});

test("company and role are required", async ({ page }) => {
  await page.getByRole("button", { name: "+ Add application" }).click();
  const dialog = page.locator("#application-dialog");
  await dialog.getByRole("button", { name: "Save" }).click();
  await expect(dialog.getByRole("alert")).toContainText("company");
  await expect(dialog.getByLabel("Company", { exact: true })).toHaveAttribute("aria-invalid", "true");
  await dialog.getByRole("button", { name: "Cancel" }).click();
  await expect(dialog).toBeHidden();
});

test("filter, edit and delete", async ({ page }) => {
  for (const company of ["Infosys", "Zensar"]) {
    await page.getByRole("button", { name: "+ Add application" }).click();
    const dialog = page.locator("#application-dialog");
    await dialog.getByLabel("Company", { exact: true }).fill(company);
    await dialog.getByLabel("Role", { exact: true }).fill("Associate Engineer");
    await dialog.getByRole("button", { name: "Save" }).click();
    await expect(dialog).toBeHidden();
  }
  await expect(page.locator(".app-card")).toHaveCount(2);
  await page.getByLabel("Filter applications", { exact: true }).fill("zen");
  await expect(page.locator(".app-card")).toHaveCount(1);
  await page.getByLabel("Filter applications", { exact: true }).fill("");

  await page.locator(".app-card", { hasText: "Infosys" }).getByRole("button", { name: "Details" }).click();
  const dialog = page.locator("#application-dialog");
  await dialog.getByLabel("Role", { exact: true }).fill("Systems Engineer");
  await dialog.getByRole("button", { name: "Save" }).click();
  await expect(page.locator(".app-card", { hasText: "Infosys" })).toContainText("Systems Engineer");

  page.once("dialog", (d) => d.accept());
  await page.locator(".app-card", { hasText: "Infosys" }).getByRole("button", { name: "Details" }).click();
  await dialog.getByRole("button", { name: "Delete" }).click();
  await expect(page.locator(".app-card")).toHaveCount(1);

  await page.getByRole("button", { name: "Dashboard" }).click();
  await expect(page.locator('#view-dashboard [data-k="totalApplications"]')).toHaveText("1");
});
