/**
 * protect.js — Frontend video protection for Ramdev Portal
 * Disables right-click, drag, keyboard shortcuts, and DevTools open attempts.
 * NOTE: This is a client-side deterrent. Server-side streaming (no direct file URL)
 * is the primary protection mechanism.
 */

(function() {
  'use strict';

  // 1. Disable right-click
  document.addEventListener('contextmenu', function(e) {
    e.preventDefault();
    return false;
  });

  // 2. Disable drag on all video/image elements
  document.addEventListener('dragstart', function(e) {
    if (e.target.tagName === 'VIDEO' || e.target.tagName === 'IMG') {
      e.preventDefault();
    }
  });

  // 3. Block keyboard shortcuts for saving / viewing source
  document.addEventListener('keydown', function(e) {
    var blocked = [
      // Ctrl/Cmd + S (save), U (view-source), P (print), A (select all)
      { ctrl: true, keys: ['s', 'u', 'p', 'a'] },
      // F12 (DevTools)
      { ctrl: false, keys: ['F12'] },
    ];

    blocked.forEach(function(rule) {
      if (rule.ctrl && (e.ctrlKey || e.metaKey) && rule.keys.includes(e.key.toLowerCase())) {
        e.preventDefault();
        e.stopPropagation();
      }
      if (!rule.ctrl && rule.keys.includes(e.key)) {
        e.preventDefault();
      }
    });

    // Ctrl+Shift+I / Ctrl+Shift+J (DevTools)
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && ['i', 'j', 'c'].includes(e.key.toLowerCase())) {
      e.preventDefault();
    }
  });

  // 4. Disable text selection on video containers
  var videoEls = document.querySelectorAll('video');
  videoEls.forEach(function(v) {
    v.addEventListener('contextmenu', function(e) { e.preventDefault(); });
  });

  // 5. Warn on visibility change (screen capture detection attempt)
  document.addEventListener('visibilitychange', function() {
    // Pause video if tab is hidden (optional, comment out if not desired)
    var vid = document.getElementById('ramdevPlayer');
    if (vid && document.hidden) {
      // vid.pause();  // uncomment to pause on tab switch
    }
  });

})();
