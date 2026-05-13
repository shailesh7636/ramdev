// Mobile sidebar — works alongside inline layout.html toggleSidebar()
document.addEventListener('DOMContentLoaded', function() {
  // Close sidebar when clicking outside on mobile (fallback)
  document.addEventListener('click', function(e) {
    var sidebar   = document.getElementById('sidebar');
    var overlay   = document.getElementById('sidebarOverlay');
    var toggleBtn = document.getElementById('menuToggle');
    if (sidebar && sidebar.classList.contains('open')) {
      if (!sidebar.contains(e.target) && e.target !== toggleBtn && !toggleBtn.contains(e.target)) {
        sidebar.classList.remove('open');
        if (overlay) overlay.classList.remove('visible');
        document.body.style.overflow = '';
      }
    }
  });
});
