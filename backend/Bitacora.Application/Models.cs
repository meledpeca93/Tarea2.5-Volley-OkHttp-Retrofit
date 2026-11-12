using System.ComponentModel.DataAnnotations;
using Bitacora.Domain;
namespace Bitacora.Application;

public sealed class RegistroInput : IValidatableObject
{
    [Required, StringLength(100, MinimumLength = 3)] public string Titulo { get; init; } = "";
    [Required, StringLength(500)] public string Descripcion { get; init; } = "";
    public string? FotoBase64 { get; init; }
    public IEnumerable<ValidationResult> Validate(ValidationContext context) =>
        Registro.Validate(Titulo, Descripcion, FotoBase64)
            .Select(e => new ValidationResult(e.Value[0], [e.Key]));
}
public sealed record RegistroListItem(int Id, string Titulo, string Descripcion, DateTime FechaCreacion);
public sealed record RegistroDetail(int Id, string Titulo, string Descripcion, string? FotoBase64, DateTime FechaCreacion);
