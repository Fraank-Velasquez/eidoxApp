function abrirModalPremium() {
    const backdrop = document.getElementById('modalPremiumBackdrop');
    if (!backdrop) return;
    backdrop.hidden = false;
    document.body.style.overflow = 'hidden';
}

function cerrarModalPremium() {
    const backdrop = document.getElementById('modalPremiumBackdrop');
    if (!backdrop) return;
    backdrop.hidden = true;
    document.body.style.overflow = '';
}

document.addEventListener('DOMContentLoaded', () => {
    const backdrop = document.getElementById('modalPremiumBackdrop');
    const botonCerrar = document.getElementById('modalPremiumCerrar');

    if (botonCerrar) {
        botonCerrar.addEventListener('click', cerrarModalPremium);
    }

    if (backdrop) {
        backdrop.addEventListener('click', (evento) => {
            if (evento.target === backdrop) {
                cerrarModalPremium();
            }
        });
    }

    document.addEventListener('keydown', (evento) => {
        if (evento.key === 'Escape') {
            cerrarModalPremium();
        }
    });

    document.querySelectorAll('.modal-premium-toggle-opcion').forEach((boton) => {
        boton.addEventListener('click', () => {
            document.querySelectorAll('.modal-premium-toggle-opcion').forEach((b) => b.classList.remove('activo'));
            boton.classList.add('activo');
        });
    });
});