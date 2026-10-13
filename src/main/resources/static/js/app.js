// Placement Prep Tracker - single-page front end (no framework, no inline scripts: CSP friendly).
import { api, setToken, getToken, onUnauthorized } from "./api.js";
import { previewInterval } from "./sm2.js";

const STAGES = [
  ["WISHLIST", "Wishlist"], ["APPLIED", "Applied"], ["OA", "Online test"],
  ["INTERVIEW", "Interview"], ["OFFER", "Offer"], ["REJECTED", "Rejected"],
];
const STAGE_LABEL = Object.fromEntries(STAGES);
const SOURCES = [["LINKEDIN", "LinkedIn"], ["NAUKRI", "Naukri"], ["INDEED", "Indeed"], ["CAMPUS", "Campus"],
  ["REFERRAL", "Referral"], ["COMPANY_SITE", "Company site"], ["OTHER", "Other"]];
const SOURCE_LABEL = Object.fromEntries(SOURCES);
const PLATFORMS = [["LEETCODE", "LeetCode"], ["GFG", "GeeksforGeeks"], ["HACKERRANK", "HackerRank"],
  ["CODECHEF", "CodeChef"], ["CODEFORCES", "Codeforces"], ["OTHER", "Other"]];
const DIFFICULTIES = [["EASY", "Easy"], ["MEDIUM", "Medium"], ["HARD", "Hard"]];
const DIFFICULTY_LABEL = Object.fromEntries(DIFFICULTIES);
const TOPICS = [["ARRAYS", "Arrays"], ["STRINGS", "Strings"], ["HASHING", "Hashing"], ["TWO_POINTERS", "Two pointers"],
  ["SLIDING_WINDOW", "Sliding window"], ["BINARY_SEARCH", "Binary search"], ["STACK_QUEUE", "Stack & queue"],
  ["LINKED_LIST", "Linked list"], ["TREES", "Trees"], ["GRAPHS", "Graphs"], ["HEAPS", "Heaps"], ["GREEDY", "Greedy"],
  ["RECURSION", "Recursion"], ["DP", "Dynamic programming"], ["SQL", "SQL"], ["OTHER", "Other"]];
const TOPIC_LABEL = Object.fromEntries(TOPICS);
const GRADES = [
  { quality: 1, label: "Forgot", cls: "forgot" },
  { quality: 3, label: "Hard", cls: "hard" },
  { quality: 4, label: "Good", cls: "good" },
  { quality: 5, label: "Easy", cls: "easy" },
];

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

/** Builds DOM nodes; text is always set as textContent, so user data can never inject HTML. */
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === null || v === undefined || v === false) continue;
    if (k === "class") el.className = v;
    else if (k === "dataset") Object.assign(el.dataset, v);
    else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
    else if (v === true) el.setAttribute(k, "");
    else el.setAttribute(k, v);
  }
  for (const c of children.flat()) {
    if (c === null || c === undefined || c === false) continue;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  }
  return el;
}

const state = { me: null, view: "dashboard", applications: [], problems: [], due: [], dashboard: null };

// ---------- dates ----------
const parseDate = (iso) => { const [y, m, d] = iso.split("-").map(Number); return new Date(y, m - 1, d); };
const fmtDate = (iso, opts = { day: "numeric", month: "short" }) => parseDate(iso).toLocaleDateString("en-IN", opts);
function daysFrom(todayIso, iso) {
  return Math.round((parseDate(iso) - parseDate(todayIso)) / 86400000);
}
function relative(days) {
  if (days === 0) return "today";
  if (days === 1) return "tomorrow";
  if (days === -1) return "yesterday";
  return days > 0 ? `in ${days} days` : `${-days} days ago`;
}
function localToday() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
const todayIso = () => state.dashboard?.today ?? localToday();

// ---------- toasts ----------
function toast(message, kind = "ok") {
  const el = h("div", { class: `toast ${kind === "error" ? "error" : ""}`, role: kind === "error" ? "alert" : "status" }, message);
  $("#toasts").append(el);
  setTimeout(() => el.remove(), 3200);
}

// ---------- forms ----------
function fillSelect(select, options, { blank } = {}) {
  select.replaceChildren(...(blank ? [h("option", { value: "" }, blank)] : []),
    ...options.map(([v, l]) => h("option", { value: v }, l)));
}
function formData(form) {
  const data = {};
  for (const el of form.elements) {
    if (!el.name) continue;
    const v = el.value.trim();
    data[el.name] = v === "" ? null : v;
  }
  return data;
}
function showErrors(form, err) {
  $$("[aria-invalid]", form).forEach((el) => el.removeAttribute("aria-invalid"));
  const fields = err.fields ?? {};
  for (const name of Object.keys(fields)) form.elements[name]?.setAttribute("aria-invalid", "true");
  const detail = Object.entries(fields).map(([k, v]) => `${k} ${v}`).join("; ");
  $(".form-error", form).textContent = detail ? `${err.message}: ${detail}` : err.message;
}
function clearErrors(form) {
  $$("[aria-invalid]", form).forEach((el) => el.removeAttribute("aria-invalid"));
  $(".form-error", form).textContent = "";
}
async function busy(button, fn) {
  button.disabled = true;
  try { return await fn(); } finally { button.disabled = false; }
}

// ---------- auth ----------
function showAuth() {
  state.me = null;
  switchAuthTab(false);
  $(".topbar").hidden = true;
  $$(".view").forEach((v) => { v.hidden = true; });
  $("#view-auth").hidden = false;
}
onUnauthorized(() => { setToken(null); showAuth(); });

function switchAuthTab(register) {
  $("#tab-login").setAttribute("aria-selected", String(!register));
  $("#tab-register").setAttribute("aria-selected", String(register));
  $("#login-form").hidden = register;
  $("#register-form").hidden = !register;
}

async function signIn(path, body, form) {
  clearErrors(form);
  try {
    const res = await api(path, { method: "POST", body, auth: false });
    setToken(res.token);
    await start(res.user);
  } catch (err) {
    showErrors(form, err);
  }
}

// ---------- navigation ----------
async function go(view) {
  state.view = view;
  if (location.hash !== `#${view}`) history.replaceState(null, "", `#${view}`);
  $$(".tab").forEach((t) => {
    if (t.dataset.view === view) t.setAttribute("aria-current", "page"); else t.removeAttribute("aria-current");
  });
  $$(".view").forEach((v) => { v.hidden = v.id !== `view-${view}`; });
  if (view === "dashboard") await loadDashboard();
  if (view === "applications") await loadApplications();
  if (view === "problems") await loadProblems();
}

async function start(user) {
  state.me = user ?? await api("/api/me");
  $("#view-auth").hidden = true;
  $(".topbar").hidden = false;
  $("#who").textContent = state.me.name;
  $("#goal-label").textContent = state.me.dailyGoal;
  const wanted = location.hash.slice(1);
  await go(["dashboard", "applications", "problems"].includes(wanted) ? wanted : "dashboard");
}

// ---------- dashboard ----------
async function loadDashboard() {
  // Fill the card's width with weeks, like GitHub's contribution graph (18px per column).
  const width = $("#view-dashboard .heatmap-wrap")?.clientWidth || 900;
  const weeks = Math.max(8, Math.min(53, Math.floor((width - 40) / 18)));
  const d = await api(`/api/dashboard?weeks=${weeks}`);
  state.dashboard = d;
  renderDashboard(d);
  updateDueBadge(d.dueCount);
}

function updateDueBadge(count) {
  const badge = $("#due-badge");
  badge.hidden = !count;
  badge.textContent = count;
}

function renderDashboard(d) {
  const day = parseDate(d.today);
  $("#today-label").textContent = `${day.toLocaleDateString("en-GB", { weekday: "long" })}, `
    + day.toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" });
  const set = (k, v) => $$(`#view-dashboard [data-k="${k}"]`).forEach((el) => { el.textContent = v; });
  for (const k of ["todayCount", "dailyGoal", "currentStreak", "longestStreak", "totalSolved", "dueCount",
    "activeApplications", "totalApplications", "offers"]) set(k, d[k]);
  const remaining = Math.max(0, d.dailyGoal - d.todayCount);
  set("todayNote", remaining === 0 ? "Goal reached - nice work!" : `${remaining} more to hit your goal`);
  const ratio = Math.min(1, d.todayCount / Math.max(1, d.dailyGoal));
  $(".ring-fill").style.strokeDashoffset = String(113.1 * (1 - ratio));

  $("#difficulty-split").replaceChildren(...DIFFICULTIES.map(([k, l]) =>
    h("span", { class: `pill ${k.toLowerCase()}`, title: `${d.byDifficulty[k] ?? 0} ${l.toLowerCase()}` },
      `${l[0]} ${d.byDifficulty[k] ?? 0}`)));

  renderHeatmap(d.heatmap, d.today);
  renderFunnel(d.funnel);
  renderTopics(d.byTopic);
  renderUpcoming(d.upcoming, d.today);
  $("#by-stage").replaceChildren(...STAGES.map(([k, l]) =>
    h("div", { class: "stage-tile" }, h("strong", {}, d.byStage[k] ?? 0), h("span", {}, h("i", { class: `dot ${k}` }), " ", l))));
}

function renderHeatmap(grid, today) {
  const root = $("#heatmap");
  const months = h("div", { class: "months" });
  let lastMonth = -1;
  grid.weeks.forEach((week, i) => {
    const first = parseDate(week[0].date);
    const label = first.getMonth() !== lastMonth && first.getDate() <= 7 ? first.toLocaleDateString("en-IN", { month: "short" }) : "";
    if (label) lastMonth = first.getMonth();
    months.append(h("span", { "data-col": i }, label));
  });
  const days = h("div", { class: "days" }, ...["Mon", "", "Wed", "", "Fri", "", ""].map((d) => h("span", {}, d)));
  const cells = h("div", { class: "cells" });
  for (const week of grid.weeks) {
    for (const cell of week) {
      if (cell.count === null) {
        cells.append(h("span", { class: "cell future" }));
        continue;
      }
      const title = `${cell.count} ${cell.count === 1 ? "activity" : "activities"} on ${fmtDate(cell.date, { day: "numeric", month: "short", year: "numeric" })}`;
      cells.append(h("span", { class: `cell l${cell.level}${cell.date === today ? " is-today" : ""}`, title, "data-date": cell.date, "data-count": cell.count }));
    }
  }
  root.replaceChildren(months, days, cells);
  root.setAttribute("aria-label", `Practice heatmap: ${grid.activeDays} active days in the last ${grid.weeks.length} weeks`);
  $("#heatmap-summary").textContent = `${grid.total} problems solved or revised on ${grid.activeDays} days in the last ${grid.weeks.length} weeks`;
}

function renderFunnel(steps) {
  const max = Math.max(1, ...steps.map((s) => s.count));
  const root = $("#funnel");
  root.replaceChildren(...steps.map((s) => {
    const bar = h("div", { class: "funnel-bar" }, s.count);
    const row = h("div", { class: "funnel-row" },
      h("span", { class: "funnel-label" }, STAGE_LABEL[s.stage]),
      h("div", { class: "funnel-track" }, bar),
      h("span", { class: "funnel-rate" }, s.conversionPercent === null ? "" : `${s.conversionPercent}%`));
    requestAnimationFrame(() => { bar.style.width = `${Math.max(8, (s.count / max) * 100)}%`; });
    return row;
  }));
  if (steps.every((s) => s.count === 0)) {
    root.append(h("p", { class: "empty" }, "Add applications to see your funnel."));
    return;
  }
  const applied = steps[0].count;
  const offers = steps[steps.length - 1].count;
  const interviews = steps.find((s) => s.stage === "INTERVIEW")?.count ?? 0;
  root.append(h("p", { class: "funnel-summary" },
    h("strong", {}, `${offers} offer${offers === 1 ? "" : "s"}`), ` from ${applied} application${applied === 1 ? "" : "s"} `,
    `(${applied ? Math.round((offers * 1000) / applied) / 10 : 0}%). `,
    h("strong", {}, `${interviews}`), ` reached the interview stage.`));
}

function renderTopics(topics) {
  const root = $("#topics");
  if (!topics.length) {
    root.replaceChildren(h("p", { class: "empty" }, "Log solved problems to see topic coverage."));
    return;
  }
  const max = Math.max(...topics.map((t) => t.count));
  root.replaceChildren(...topics.slice(0, 10).map((t) => {
    const bar = h("div", { class: "bar" });
    requestAnimationFrame(() => { bar.style.width = `${(t.count / max) * 100}%`; });
    return h("div", { class: "bar-row" }, h("span", {}, TOPIC_LABEL[t.key] ?? t.key),
      h("div", { class: "bar-track" }, bar), h("span", { class: "bar-count" }, t.count));
  }));
}

function renderUpcoming(list, today) {
  const root = $("#upcoming");
  if (!list.length) {
    root.replaceChildren(h("li", { class: "empty" }, "Nothing scheduled. Add a next step to an application."));
    return;
  }
  root.replaceChildren(...list.map((a) => {
    const date = parseDate(a.nextStepOn);
    return h("li", {},
      h("span", { class: "date-chip" }, date.getDate(), h("small", {}, date.toLocaleDateString("en-IN", { month: "short" }))),
      h("span", { class: "what" }, h("strong", {}, a.company), h("span", { class: "muted small" }, a.nextStep ?? STAGE_LABEL[a.stage])),
      h("span", { class: "when" }, relative(daysFrom(today, a.nextStepOn))));
  }));
}

// ---------- applications board ----------
async function loadApplications() {
  state.applications = await api("/api/applications");
  renderBoard();
}

function renderBoard() {
  const filter = $("#board-search").value.trim().toLowerCase();
  const apps = state.applications.filter((a) => !filter
    || a.company.toLowerCase().includes(filter) || a.jobTitle.toLowerCase().includes(filter));
  const board = $("#board");
  board.replaceChildren(...STAGES.map(([stage, label]) => {
    const inStage = apps.filter((a) => a.stage === stage);
    const column = h("section", { class: "column", "data-stage": stage, "aria-label": label },
      h("div", { class: "column-head" }, h("h2", {}, h("i", { class: `dot ${stage}` }), label),
        h("span", { class: "count" }, inStage.length)),
      h("div", { class: "cards" }, ...inStage.map(appCard)));
    column.addEventListener("dragover", (e) => { e.preventDefault(); column.classList.add("drop-target"); });
    column.addEventListener("dragleave", (e) => { if (!column.contains(e.relatedTarget)) column.classList.remove("drop-target"); });
    column.addEventListener("drop", (e) => {
      e.preventDefault();
      column.classList.remove("drop-target");
      const id = Number(e.dataTransfer.getData("text/plain"));
      if (id) moveApplication(id, stage);
    });
    return column;
  }));
}

function appCard(a) {
  const move = h("select", { "aria-label": `Move ${a.company} to stage` },
    ...STAGES.map(([v, l]) => h("option", { value: v, selected: v === a.stage }, l)));
  move.addEventListener("change", () => moveApplication(a.id, move.value));
  const card = h("article", { class: "app-card", draggable: "true", "data-id": a.id },
    h("div", { class: "company" }, a.company),
    h("div", { class: "role" }, a.jobTitle),
    h("div", { class: "meta" },
      a.location && h("span", { class: "pill" }, a.location),
      h("span", { class: "pill" }, SOURCE_LABEL[a.source] ?? a.source),
      a.packageLpa !== null && h("span", { class: "pill" }, `₹${Number(a.packageLpa)} LPA`)),
    a.nextStepOn && a.stage !== "REJECTED" && h("div", { class: "next-step" },
      `${a.nextStep ?? "Next step"} · ${fmtDate(a.nextStepOn)}`),
    h("div", { class: "foot" }, move, h("button", { class: "open", type: "button", onclick: () => openApplication(a) }, "Details")));
  card.addEventListener("dragstart", (e) => {
    e.dataTransfer.setData("text/plain", String(a.id));
    e.dataTransfer.effectAllowed = "move";
    card.classList.add("dragging");
  });
  card.addEventListener("dragend", () => card.classList.remove("dragging"));
  return card;
}

async function moveApplication(id, stage) {
  const current = state.applications.find((a) => a.id === id);
  if (!current || current.stage === stage) return;
  try {
    const updated = await api(`/api/applications/${id}/stage`, { method: "PATCH", body: { stage } });
    Object.assign(current, updated);
    renderBoard();
    toast(`${updated.company} moved to ${STAGE_LABEL[stage]}`);
  } catch (err) {
    toast(err.message, "error");
    renderBoard();
  }
}

let editingApplication = null;

async function openApplication(app = null) {
  editingApplication = app;
  const dialog = $("#application-dialog");
  const form = $("#application-form");
  form.reset();
  clearErrors(form);
  $("#application-dialog-title").textContent = app ? `${app.company}` : "Add application";
  $("#delete-application").hidden = !app;
  const history = $("#history");
  history.hidden = true;
  history.replaceChildren();
  if (app) {
    for (const name of ["company", "jobTitle", "location", "source", "stage", "packageLpa", "jobUrl", "nextStep", "nextStepOn", "notes"]) {
      form.elements[name].value = app[name] ?? "";
    }
  } else {
    form.elements.stage.value = "APPLIED";
    form.elements.source.value = "LINKEDIN";
  }
  dialog.showModal();
  if (app) {
    try {
      const changes = await api(`/api/applications/${app.id}/history`);
      history.replaceChildren(h("h3", {}, "History"), h("ol", {}, ...changes.map((c) =>
        h("li", {}, `${c.from ? `${STAGE_LABEL[c.from]} → ` : "Added as "}${STAGE_LABEL[c.to]}`,
          h("span", { class: "muted" }, ` · ${new Date(c.changedAt).toLocaleDateString("en-IN", { day: "numeric", month: "short" })}`)))));
      history.hidden = false;
    } catch { /* history is optional */ }
  }
}

async function saveApplication(event) {
  event.preventDefault();
  const form = $("#application-form");
  const data = formData(form);
  if (data.packageLpa !== null) data.packageLpa = Number(data.packageLpa);
  clearErrors(form);
  try {
    await busy(event.submitter ?? $("button[type=submit]", form), async () => {
      if (editingApplication) {
        await api(`/api/applications/${editingApplication.id}`, { method: "PUT", body: data });
        toast("Application updated");
      } else {
        await api("/api/applications", { method: "POST", body: data });
        toast(`${data.company} added`);
      }
    });
    $("#application-dialog").close();
    await loadApplications();
  } catch (err) {
    showErrors(form, err);
  }
}

async function deleteApplication() {
  if (!editingApplication || !confirm(`Delete ${editingApplication.company}? Its history goes too.`)) return;
  try {
    await api(`/api/applications/${editingApplication.id}`, { method: "DELETE" });
    $("#application-dialog").close();
    toast("Application deleted");
    await loadApplications();
  } catch (err) {
    toast(err.message, "error");
  }
}

// ---------- problems ----------
async function loadProblems() {
  const params = new URLSearchParams();
  const q = $("#problem-search").value.trim();
  if (q) params.set("q", q);
  if ($("#filter-topic").value) params.set("topic", $("#filter-topic").value);
  if ($("#filter-difficulty").value) params.set("difficulty", $("#filter-difficulty").value);
  const [due, problems] = await Promise.all([api("/api/problems/due"), api(`/api/problems?${params}`)]);
  state.due = due;
  state.problems = problems;
  renderDue();
  renderProblems();
  updateDueBadge(due.length);
}

function difficultyPill(d) {
  return h("span", { class: `pill ${d.toLowerCase()}` }, DIFFICULTY_LABEL[d]);
}

function titleNode(p) {
  return p.url ? h("a", { href: p.url, target: "_blank", rel: "noopener noreferrer" }, p.title) : h("span", {}, p.title);
}

function renderDue() {
  const root = $("#due-list");
  if (!state.due.length) {
    root.replaceChildren(h("li", { class: "empty" }, "All caught up - nothing to revise today."));
    return;
  }
  root.replaceChildren(...state.due.map((p) => {
    const overdue = -p.dueInDays;
    return h("li", { class: "due-item", "data-id": p.id },
      h("div", {},
        h("div", { class: "title" }, titleNode(p)),
        h("div", { class: "meta" },
          h("span", { class: "pill" }, TOPIC_LABEL[p.topic]), difficultyPill(p.difficulty),
          overdue > 0 ? h("span", { class: "pill overdue" }, `${overdue} day${overdue === 1 ? "" : "s"} overdue`)
            : h("span", { class: "pill today" }, "due today"),
          h("span", { class: "pill" }, `revision #${p.repetitions}`))),
      h("div", { class: "grades", role: "group", "aria-label": `Rate your recall of ${p.title}` },
        ...GRADES.map((g) => h("button", {
          class: `btn ghost small grade ${g.cls}`, type: "button", "data-quality": g.quality,
          onclick: (e) => review(p, g, e.currentTarget),
        }, g.label, h("small", {}, `${previewInterval(p, g.quality)}d`)))));
  }));
}

async function review(problem, grade, button) {
  try {
    const updated = await busy(button, () => api(`/api/problems/${problem.id}/review`,
      { method: "POST", body: { quality: grade.quality } }));
    toast(`${problem.title}: next revision ${relative(daysFrom(todayIso(), updated.nextReviewOn))}`);
    await loadProblems();
  } catch (err) {
    toast(err.message, "error");
  }
}

const PAGE = 20;
let showAllProblems = false;

function renderProblems() {
  const tbody = $("#problem-table tbody");
  $("#problems-empty").hidden = state.problems.length > 0;
  const visible = showAllProblems ? state.problems : state.problems.slice(0, PAGE);
  $("#show-all")?.remove();
  if (visible.length < state.problems.length) {
    $("#problem-table").closest(".card").append(h("button", {
      class: "btn ghost small show-all", id: "show-all", type: "button",
      onclick: () => { showAllProblems = true; renderProblems(); },
    }, `Show all ${state.problems.length} problems`));
  }
  tbody.replaceChildren(...visible.map((p) => h("tr", { "data-id": p.id },
    h("td", { class: "title-cell" }, titleNode(p)),
    h("td", {}, TOPIC_LABEL[p.topic]),
    h("td", {}, difficultyPill(p.difficulty)),
    h("td", {}, fmtDate(p.solvedOn)),
    h("td", {}, p.dueInDays <= 0 ? h("span", { class: `pill ${p.dueInDays < 0 ? "overdue" : "today"}` }, p.dueInDays < 0 ? "overdue" : "today")
      : `${fmtDate(p.nextReviewOn)} (${relative(p.dueInDays)})`),
    h("td", {}, p.ease.toFixed(2)),
    h("td", {}, h("button", { class: "icon-btn", type: "button", title: "Delete", "aria-label": `Delete ${p.title}`, onclick: () => deleteProblem(p) }, "×")))));
}

async function deleteProblem(p) {
  if (!confirm(`Delete "${p.title}" and its revision history?`)) return;
  try {
    await api(`/api/problems/${p.id}`, { method: "DELETE" });
    toast("Problem deleted");
    await loadProblems();
  } catch (err) {
    toast(err.message, "error");
  }
}

function openProblemDialog() {
  const form = $("#problem-form");
  form.reset();
  clearErrors(form);
  form.elements.platform.value = "LEETCODE";
  form.elements.difficulty.value = "MEDIUM";
  form.elements.topic.value = "ARRAYS";
  form.elements.solvedOn.value = todayIso();
  form.elements.solvedOn.max = todayIso();
  $("#problem-dialog").showModal();
}

async function saveProblem(event) {
  event.preventDefault();
  const form = $("#problem-form");
  clearErrors(form);
  try {
    const created = await busy(event.submitter ?? $("button[type=submit]", form),
      () => api("/api/problems", { method: "POST", body: formData(form) }));
    $("#problem-dialog").close();
    toast(`Logged "${created.title}" - first revision ${relative(created.dueInDays)}`);
    if (state.view === "problems") await loadProblems(); else await go("problems");
  } catch (err) {
    showErrors(form, err);
  }
}

// ---------- goal ----------
function openGoal() {
  const form = $("#goal-form");
  clearErrors(form);
  form.elements.dailyGoal.value = state.me.dailyGoal;
  $("#goal-dialog").showModal();
}

async function saveGoal(event) {
  event.preventDefault();
  const form = $("#goal-form");
  try {
    state.me = await api("/api/me/goal", { method: "PUT", body: { dailyGoal: Number(form.elements.dailyGoal.value) } });
    $("#goal-label").textContent = state.me.dailyGoal;
    $("#goal-dialog").close();
    toast(`Daily goal set to ${state.me.dailyGoal}`);
    if (state.view === "dashboard") await loadDashboard();
  } catch (err) {
    showErrors(form, err);
  }
}

// ---------- wiring ----------
function debounce(fn, ms) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

function wire() {
  fillSelect($("#application-form").elements.source, SOURCES);
  fillSelect($("#application-form").elements.stage, STAGES);
  fillSelect($("#problem-form").elements.platform, PLATFORMS);
  fillSelect($("#problem-form").elements.difficulty, DIFFICULTIES);
  fillSelect($("#problem-form").elements.topic, TOPICS);
  fillSelect($("#filter-topic"), TOPICS, { blank: "All topics" });
  fillSelect($("#filter-difficulty"), DIFFICULTIES, { blank: "All levels" });

  $("#tab-login").addEventListener("click", () => switchAuthTab(false));
  $("#tab-register").addEventListener("click", () => switchAuthTab(true));
  $("#login-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const f = e.currentTarget;
    signIn("/api/auth/login", { email: f.elements.email.value, password: f.elements.password.value }, f);
  });
  $("#demo-login").addEventListener("click", () =>
    signIn("/api/auth/login", { email: "demo@prep.in", password: "demo1234" }, $("#login-form")));
  $("#register-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const f = e.currentTarget;
    signIn("/api/auth/register", { name: f.elements.name.value, email: f.elements.email.value, password: f.elements.password.value }, f);
  });
  $("#logout").addEventListener("click", () => { setToken(null); history.replaceState(null, "", "#"); showAuth(); });

  $$(".tab").forEach((t) => t.addEventListener("click", () => go(t.dataset.view)));
  window.addEventListener("hashchange", () => {
    const v = location.hash.slice(1);
    if (state.me && v !== state.view && ["dashboard", "applications", "problems"].includes(v)) go(v);
  });
  $("#start-revision").addEventListener("click", () => go("problems"));

  $("#add-application").addEventListener("click", () => openApplication());
  $("#application-form").addEventListener("submit", saveApplication);
  $("#delete-application").addEventListener("click", deleteApplication);
  $("#board-search").addEventListener("input", renderBoard);

  $("#add-problem").addEventListener("click", openProblemDialog);
  $("#problem-form").addEventListener("submit", saveProblem);
  const reload = debounce(() => loadProblems().catch((e) => toast(e.message, "error")), 250);
  $("#problem-search").addEventListener("input", reload);
  $("#filter-topic").addEventListener("change", reload);
  $("#filter-difficulty").addEventListener("change", reload);

  $("#goal-button").addEventListener("click", openGoal);
  $("#goal-form").addEventListener("submit", saveGoal);

  // "Cancel" buttons close their dialog without validation.
  $$("dialog button[value=cancel]").forEach((b) => b.addEventListener("click", (e) => {
    e.preventDefault();
    b.closest("dialog").close();
  }));
}

wire();
if (getToken()) {
  start().catch(() => showAuth());
} else {
  showAuth();
}
