// Keep end date >= start date and not before today (UI guard; service also validates)
(function () {
    const start = document.getElementById('startDate');
    const end = document.getElementById('endDate');

    function todayStr() {
        return new Date().toISOString().slice(0, 10);
    }

    function syncMin() {
        const min = start.value || todayStr();
        end.min = min;
        if (end.value && end.value < min) end.value = min;
    }

    if (start && end) {
        start.addEventListener('change', syncMin);
        syncMin();
    }

    // Auto-close any flash alerts after ~4s
    setTimeout(() => {
        document.querySelectorAll('.alert').forEach(el => {
            try {
                const inst = bootstrap.Alert.getOrCreateInstance(el);
                inst.close();
            } catch (_) {}
        });
    }, 4000);

    // Show spinner + overlay on submit to mask email-sending delay
    const form = document.getElementById('vacationForm');
    const btn = document.getElementById('bookBtn');
    const btnSpinner = document.getElementById('bookBtnSpinner');
    const overlay = document.getElementById('loadingOverlay');

    if (form && btn && btnSpinner && overlay) {
        form.addEventListener('submit', () => {
            // Guard: only proceed if form is valid (browser-side)
            if (!form.checkValidity()) return;

            btn.disabled = true;
            btnSpinner.classList.remove('d-none');
            overlay.classList.remove('d-none');
        });
    }
})();
