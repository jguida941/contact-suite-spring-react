import { useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { useMediaQuery } from '@/hooks/useMediaQuery';

const pageTitles: Record<string, string> = {
  '/': 'Overview',
  '/contacts': 'Contacts',
  '/tasks': 'Tasks',
  '/appointments': 'Appointments',
  '/settings': 'Settings',
  '/help': 'Help',
};

export function AppShell() {
  const location = useLocation();
  const isDesktop = useMediaQuery('(min-width: 1024px)');
  const isTablet = useMediaQuery('(min-width: 768px)');
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const title = pageTitles[location.pathname] || 'ContactApp';

  // Desktop: full sidebar
  // Tablet: icons-only sidebar
  // Mobile: no sidebar (bottom nav would be added later)
  const showSidebar = isTablet;
  const sidebarCollapsed = !isDesktop;

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Sidebar */}
      {showSidebar && (
        <Sidebar collapsed={sidebarCollapsed && sidebarOpen} />
      )}

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <TopBar
          title={title}
          showMenuButton={!isTablet}
          onMenuClick={() => setSidebarOpen(!sidebarOpen)}
        />
        <main className="flex-1 overflow-auto p-4 md:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
