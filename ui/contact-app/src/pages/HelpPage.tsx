import { ExternalLink, Book, Code, MessageCircle, FileText } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';

const helpSections = [
  {
    icon: Book,
    title: 'Getting Started',
    description: 'Learn the basics of using ContactApp.',
    items: [
      { label: 'Create your first contact', description: 'Go to Contacts and click "Add Contact"' },
      { label: 'Schedule an appointment', description: 'Navigate to Appointments and add a new entry' },
      { label: 'Track your tasks', description: 'Use the Tasks page to manage your to-dos' },
    ],
  },
  {
    icon: FileText,
    title: 'Features',
    description: 'Explore what you can do with ContactApp.',
    items: [
      { label: 'Contacts', description: 'Store names, phone numbers, and addresses' },
      { label: 'Tasks', description: 'Track tasks with names and descriptions' },
      { label: 'Appointments', description: 'Schedule appointments with dates and notes' },
      { label: 'Dark Mode', description: 'Toggle between light and dark themes' },
      { label: 'Color Themes', description: 'Choose from 5 different color schemes' },
    ],
  },
];

const resources = [
  {
    icon: Code,
    title: 'API Documentation',
    description: 'Swagger UI for the REST API',
    href: '/swagger-ui.html',
  },
  {
    icon: FileText,
    title: 'OpenAPI Spec',
    description: 'Machine-readable API specification',
    href: '/v3/api-docs',
  },
  {
    icon: MessageCircle,
    title: 'Health Check',
    description: 'Application health status',
    href: '/actuator/health',
  },
];

export function HelpPage() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Help</h2>
        <p className="text-muted-foreground">
          Learn how to use ContactApp and find useful resources.
        </p>
      </div>

      <Separator />

      {/* Help Sections */}
      <div className="grid gap-6 md:grid-cols-2">
        {helpSections.map((section) => (
          <Card key={section.title}>
            <CardHeader>
              <div className="flex items-center gap-2">
                <section.icon className="h-5 w-5 text-primary" />
                <CardTitle>{section.title}</CardTitle>
              </div>
              <CardDescription>{section.description}</CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-3">
                {section.items.map((item) => (
                  <li key={item.label} className="space-y-0.5">
                    <p className="text-sm font-medium">{item.label}</p>
                    <p className="text-sm text-muted-foreground">{item.description}</p>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Resources */}
      <Card>
        <CardHeader>
          <CardTitle>Developer Resources</CardTitle>
          <CardDescription>
            Technical documentation and endpoints.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            {resources.map((resource) => (
              <a
                key={resource.title}
                href={resource.href}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-start gap-3 rounded-lg border border-border p-4 transition-colors hover:bg-accent"
              >
                <resource.icon className="h-5 w-5 text-primary shrink-0 mt-0.5" />
                <div className="flex-1 space-y-1">
                  <div className="flex items-center gap-1">
                    <p className="text-sm font-medium">{resource.title}</p>
                    <ExternalLink className="h-3 w-3 text-muted-foreground" />
                  </div>
                  <p className="text-sm text-muted-foreground">{resource.description}</p>
                </div>
              </a>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Keyboard Shortcuts */}
      <Card>
        <CardHeader>
          <CardTitle>Keyboard Shortcuts</CardTitle>
          <CardDescription>
            Quick navigation tips.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-2 sm:grid-cols-2">
            <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
              <span className="text-sm">Toggle Dark Mode</span>
              <kbd className="rounded bg-muted px-2 py-1 text-xs font-mono">Click moon/sun icon</kbd>
            </div>
            <div className="flex items-center justify-between rounded-md border border-border px-3 py-2">
              <span className="text-sm">Navigate Pages</span>
              <kbd className="rounded bg-muted px-2 py-1 text-xs font-mono">Use sidebar links</kbd>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
