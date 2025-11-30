import { useState, useEffect } from 'react';

const themes = ['slate', 'ocean', 'forest', 'violet', 'zinc'] as const;
type Theme = (typeof themes)[number];

const THEME_KEY = 'contact-app-theme';
const DARK_MODE_KEY = 'contact-app-dark-mode';

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(() => {
    const stored = localStorage.getItem(THEME_KEY);
    return (stored as Theme) || 'slate';
  });

  const [darkMode, setDarkModeState] = useState<boolean>(() => {
    const stored = localStorage.getItem(DARK_MODE_KEY);
    if (stored !== null) {
      return stored === 'true';
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  useEffect(() => {
    const root = document.documentElement;

    // Remove all theme classes
    themes.forEach((t) => root.classList.remove(`theme-${t}`));

    // Add current theme class (skip slate as it's the default)
    if (theme !== 'slate') {
      root.classList.add(`theme-${theme}`);
    }

    // Handle dark mode
    if (darkMode) {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }

    // Persist to localStorage
    localStorage.setItem(THEME_KEY, theme);
    localStorage.setItem(DARK_MODE_KEY, String(darkMode));
  }, [theme, darkMode]);

  const setTheme = (newTheme: Theme) => {
    setThemeState(newTheme);
  };

  const toggleDarkMode = () => {
    setDarkModeState((prev) => !prev);
  };

  return {
    theme,
    setTheme,
    themes,
    darkMode,
    toggleDarkMode,
  };
}
