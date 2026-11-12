namespace Bitacora.Domain;

public sealed class Registro
{
    public const int MaxPhotoBytes = 2 * 1024 * 1024;
    public int Id { get; private set; }
    public string Titulo { get; private set; } = "";
    public string Descripcion { get; private set; } = "";
    public string? FotoBase64 { get; private set; }
    public DateTime FechaCreacion { get; private set; }
    private Registro() { } // EF Core
    public Registro(string titulo, string descripcion, string? fotoBase64)
    {
        Actualizar(titulo, descripcion, fotoBase64);
        FechaCreacion = DateTime.UtcNow;
    }
    public void Actualizar(string titulo, string descripcion, string? fotoBase64)
    {
        var errors = Validate(titulo, descripcion, fotoBase64);
        if (errors.Count > 0) throw new RegistroValidationException(errors);
        Titulo = titulo.Trim();
        Descripcion = descripcion.Trim();
        FotoBase64 = string.IsNullOrEmpty(fotoBase64) ? null : fotoBase64;
    }
    public static Dictionary<string, string[]> Validate(string? titulo, string? descripcion, string? foto)
    {
        var errors = new Dictionary<string, string[]>();
        if (string.IsNullOrWhiteSpace(titulo) || titulo.Length is < 3 or > 100 || titulo.Trim().Length < 3)
            errors["titulo"] = ["El título es obligatorio y debe tener entre 3 y 100 caracteres."];
        if (string.IsNullOrWhiteSpace(descripcion) || descripcion.Length > 500)
            errors["descripcion"] = ["La descripción es obligatoria y no puede superar 500 caracteres."];
        if (!string.IsNullOrEmpty(foto))
        {
            // Bound allocation before decoding; only plain Base64 is accepted.
            if (foto.Length > ((MaxPhotoBytes + 2) / 3) * 4)
                errors["fotoBase64"] = ["La foto no puede superar 2 MiB decodificados."];
            else
            {
                try
                {
                    var bytes = Convert.FromBase64String(foto);
                    if (bytes.Length == 0 || bytes.Length > MaxPhotoBytes)
                        errors["fotoBase64"] = ["La foto debe contener entre 1 byte y 2 MiB decodificados."];
                }
                catch (FormatException) { errors["fotoBase64"] = ["La foto debe ser Base64 válido, sin prefijo data:."]; }
            }
        }
        return errors;
    }
}
public sealed class RegistroValidationException(Dictionary<string, string[]> errors) : Exception("Registro inválido")
{
    public Dictionary<string, string[]> Errors { get; } = errors;
}
