document.addEventListener('DOMContentLoaded', function () {
    const checkboxes = document.querySelectorAll('.seat-checkbox');
    const quantityInput = document.getElementById('quantityInput');
    const selectedText = document.getElementById('selectedSeatText');
    const selectedCount = document.getElementById('selectedCount');

    function refreshSelection() {
        const selected = Array.from(checkboxes)
            .filter(box => box.checked)
            .map(box => box.value);

        if (quantityInput) {
            quantityInput.value = selected.length;
        }
        if (selectedText) {
            selectedText.textContent = selected.length > 0 ? selected.join(', ') : '未选择';
        }
        if (selectedCount) {
            selectedCount.textContent = selected.length;
        }

        checkboxes.forEach(box => {
            const label = box.closest('.seat-item');
            if (label) {
                label.classList.toggle('selected', box.checked);
            }
        });
    }

    checkboxes.forEach(box => box.addEventListener('change', refreshSelection));
    refreshSelection();
});
