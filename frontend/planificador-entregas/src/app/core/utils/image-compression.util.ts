/**
 * Comprime una imagen en el navegador antes de subirla.
 * Evita que fotos de celular (a veces 8-20MB) superen el límite del backend
 * y acelera la subida en conexiones móviles.
 */
export async function compressImage(
  file: File,
  maxDimension = 1600,
  quality = 0.82,
  skipIfUnderBytes = 1_200_000
): Promise<File> {
  if (!file.type.startsWith('image/') || file.size <= skipIfUnderBytes) {
    return file;
  }

  try {
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) return file;
    ctx.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();

    const blob: Blob | null = await new Promise(resolve =>
      canvas.toBlob(resolve, 'image/jpeg', quality)
    );
    if (!blob || blob.size >= file.size) return file;

    const newName = file.name.replace(/\.\w+$/, '') + '.jpg';
    return new File([blob], newName, { type: 'image/jpeg' });
  } catch {
    return file;
  }
}
