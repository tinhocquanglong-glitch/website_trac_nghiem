(function () {
    "use strict";

    var searchInput = document.getElementById("classStudentSearch");
    if (!searchInput) {
        return;
    }

    var status = document.getElementById("classStudentSearchStatus");
    var rows = Array.prototype.slice.call(document.querySelectorAll(".tracking-table tbody tr"))
            .filter(function (row) {
                return row.querySelector('input[name$=".fullName"]');
            });

    function normalize(value) {
        return (value || "")
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .replace(/đ/g, "d")
                .replace(/Đ/g, "D")
                .toLowerCase()
                .replace(/\s+/g, " ")
                .trim();
    }

    searchInput.addEventListener("input", function () {
        var query = normalize(searchInput.value);
        var visibleCount = 0;
        rows.forEach(function (row) {
            var nameInput = row.querySelector('input[name$=".fullName"]');
            var visible = !query || normalize(nameInput.value).indexOf(query) !== -1;
            row.hidden = !visible;
            if (visible) {
                visibleCount++;
            }
        });
        status.textContent = query ? visibleCount + " học sinh phù hợp" : "";
    });
}());
