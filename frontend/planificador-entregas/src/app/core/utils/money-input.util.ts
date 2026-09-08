/**
 * Formatea un input de dinero con separador de miles (es-CO) en cada
 * keystroke, preservando la posición del cursor.
 *
 * Sin esto, reasignar `input.value` en cada tecla manda el cursor al
 * final del campo — si el usuario estaba corrigiendo un dígito en medio
 * del número, los siguientes caracteres se insertan en el lugar
 * equivocado y el valor visible queda incompleto/desordenado.
 */
export function formatMoneyInput(input: HTMLInputElement, maxDigits = 13): { formatted: string; numeric: number | null } {
  const caret = input.selectionStart ?? input.value.length;
  const digitsBeforeCaret = input.value.slice(0, caret).replace(/[^0-9]/g, '').length;

  let raw = input.value.replace(/[^0-9]/g, '');
  if (raw.length > maxDigits) raw = raw.slice(0, maxDigits);
  const formatted = raw ? Number(raw).toLocaleString('es-CO') : '';

  input.value = formatted;

  let digitsSeen = 0;
  let newCaret = formatted.length;
  if (digitsBeforeCaret === 0) {
    newCaret = 0;
  } else {
    for (let i = 0; i < formatted.length; i++) {
      if (/[0-9]/.test(formatted[i])) digitsSeen++;
      if (digitsSeen === digitsBeforeCaret) {
        newCaret = i + 1;
        break;
      }
    }
  }
  input.setSelectionRange(newCaret, newCaret);

  return { formatted, numeric: raw ? Number(raw) : null };
}
