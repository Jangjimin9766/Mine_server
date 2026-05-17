import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = process.argv[2];

if (!inputPath) {
  console.error("Usage: node inspect_tc_workbook.mjs <xlsx-path>");
  process.exit(1);
}

const input = await FileBlob.load(inputPath);
const workbook = await SpreadsheetFile.importXlsx(input);

const summary = await workbook.inspect({
  kind: "workbook,sheet,table",
  maxChars: 10000,
  tableMaxRows: 8,
  tableMaxCols: 12,
  tableMaxCellChars: 100,
});

console.log("=== SUMMARY ===");
console.log(summary.ndjson);

const sheets = await workbook.inspect({
  kind: "sheet",
  include: "id,name",
  maxChars: 2000,
});

console.log("=== SHEETS ===");
console.log(sheets.ndjson);

const matches = await workbook.inspect({
  kind: "match",
  searchTerm: "test result",
  maxChars: 4000,
  options: {
    caseSensitive: false,
    maxResults: 20,
  },
});

console.log("=== MATCH:test result ===");
console.log(matches.ndjson);
