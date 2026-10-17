/* Pruebas del núcleo de Mi plata (sin navegador ni Android).
   Uso:  node probar_nucleo.js        (desde la carpeta donde está index.html)
   Extrae del index.html las funciones puras (desde "const MS_DIA" hasta el módulo "03 · ESTADO"),
   y comprueba el cálculo diario, el objetivo diario y la generación de Excel y PDF. */
const fs = require('fs'), assert = require('assert');
const html = fs.readFileSync('index.html', 'utf8');
const js = html.slice(html.indexOf('<script>') + 8, html.lastIndexOf('</script>'));
const ini = js.indexOf('const MS_DIA'), fin = js.indexOf('/* =====================================================================\n   03 · ESTADO');
assert(ini > 0 && fin > ini, 'No se encontraron las marcas del núcleo en index.html');
const VERSION_FALSA = "const VERSION = '9.x';";
eval(js.slice(ini, fin) + VERSION_FALSA + `
const S = { sueldo: 420000, previo: 0, bienvenida: true, ingresos: [{ cat: 'Sueldo', a: 420000 }],
  start: '2026-10-01', end: '2026-10-31', items: [{ n: 'Alquiler', a: 150000, p: true }],
  spent: [{ id: 1, date: '2026-10-14', amt: 12000, cat: 'Comida', note: 'Almuerzo' },
          { id: 2, date: '2026-10-16', amt: 5000, cat: 'Súper', note: 'Compras' }], recal: null, objetivo: 9000 };
const hoy = '2026-10-16';
const c = calcular(S, hoy);
assert.strictEqual(c.dias, 30);
assert.strictEqual(c.gastoHoy, 5000);
assert.strictEqual(Math.round(c.base), 9000);                         // (420000-150000)/30
const o = objetivoDiario(S, hoy);
assert.strictEqual(o.faltan, 15);
assert.strictEqual(Math.round(o.proyectado), Math.round(o.disponibleHoy - 9000 * 15));
S.objetivo = 0; assert.strictEqual(objetivoDiario(S, hoy), null);   // sin objetivo no hay tarjeta
S.objetivo = 9000;
// El objetivo como límite oficial se apoya en la recalibración: desde hoy cada día vale el objetivo
S.recal = { day: c.hoyIdx, base: 10000, obj: true };
const c2 = calcular(S, hoy);
assert.strictEqual(c2.baseHoy, 10000);
assert.strictEqual(c2.quedaHoy, 10000 - 5000);                          // objetivo - gastado hoy
assert.strictEqual(c2.manana, 10000 + (10000 - 5000));                  // lo que no se gasta pasa a mañana
S.recal = null;
// "Ahorrado vs. objetivo" cuenta solo desde el día en que se puso el objetivo
S.objetivoDesde = '2026-10-14';
const o2 = objetivoDiario(S, hoy);
assert.strictEqual(o2.diasPrevios, 2);
assert.strictEqual(o2.ahorradoAntes, (9000 - 12000) + (9000 - 0));
S.objetivoDesde = '';
const d = datosExportar(S, hoy);
const xlsx = xlsxExportar(d), pdf = pdfExportar(d);
assert.strictEqual(String.fromCharCode(xlsx[0], xlsx[1]), 'PK');       // un .xlsx es un zip
assert.strictEqual(String.fromCharCode(...pdf.slice(0, 5)), '%PDF-');
console.log('Todas las pruebas del núcleo pasaron (cálculo, objetivo como límite, ahorrado, xlsx, pdf).');
`);
