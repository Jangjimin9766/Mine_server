import fs from "node:fs/promises";
import path from "node:path";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = process.argv[2];
const outputPath = process.argv[3];

if (!inputPath || !outputPath) {
  console.error("Usage: node add_test_result_dropdown.mjs <input.xlsx> <output.xlsx>");
  process.exit(1);
}

const input = await FileBlob.load(inputPath);
const workbook = await SpreadsheetFile.importXlsx(input);
const sheet = workbook.worksheets.getItem("초매거진");

const statusListRange = "$N$4:$N$8";
const testResultRange = sheet.getRange("I4:I33");

testResultRange.dataValidation = {
  rule: {
    type: "list",
    formula1: statusListRange,
  },
};

const check = await workbook.inspect({
  kind: "table",
  range: "초매거진!A1:N10",
  include: "values,formulas",
  tableMaxRows: 10,
  tableMaxCols: 14,
});

console.log("=== CHECK ===");
console.log(check.ndjson);

const preview = await workbook.render({
  sheetName: "초매거진",
  range: "A1:N12",
  scale: 1,
  format: "png",
});

const previewDir = path.join(path.dirname(outputPath), "_preview");
await fs.mkdir(previewDir, { recursive: true });
await fs.writeFile(
  path.join(previewDir, "초매거진_dropdown_preview.png"),
  new Uint8Array(await preview.arrayBuffer()),
);

await fs.mkdir(path.dirname(outputPath), { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);

console.log(`Saved: ${outputPath}`);
