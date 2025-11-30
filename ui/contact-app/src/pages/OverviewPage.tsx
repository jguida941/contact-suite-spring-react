import { useQuery } from '@tanstack/react-query';
import { Users, CheckSquare, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { contactsApi, tasksApi, appointmentsApi } from '@/lib/api';

export function OverviewPage() {
  const { data: contacts = [] } = useQuery({
    queryKey: ['contacts'],
    queryFn: contactsApi.getAll,
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks'],
    queryFn: tasksApi.getAll,
  });

  const { data: appointments = [] } = useQuery({
    queryKey: ['appointments'],
    queryFn: appointmentsApi.getAll,
  });

  const stats = [
    {
      title: 'Total Contacts',
      value: contacts.length,
      icon: Users,
      description: 'Active contacts in your network',
    },
    {
      title: 'Active Tasks',
      value: tasks.length,
      icon: CheckSquare,
      description: 'Tasks to be completed',
    },
    {
      title: 'Upcoming Appointments',
      value: appointments.length,
      icon: Calendar,
      description: 'Scheduled meetings',
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">
          Welcome to ContactApp. Here's an overview of your data.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {stats.map((stat) => (
          <Card key={stat.title}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {stat.title}
              </CardTitle>
              <stat.icon className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="text-xs text-muted-foreground">
                {stat.description}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
