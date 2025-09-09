import { DocsLayout } from 'fumadocs-ui/layouts/docs';
import { baseOptions } from '@/lib/layout.shared';
import { source } from '@/lib/source';
import { CodeXmlIcon, CompassIcon, ServerIcon } from 'lucide-react';

function TabIcon({
  color = 'var(--color-fd-foreground)',
  children,
}: {
  color?: string;
  children: React.ReactNode;
}) {
  return (
    <div
      className="[&_svg]:size-full rounded-lg size-full text-(--tab-color) max-md:bg-(--tab-color)/10 max-md:border max-md:p-1.5"
      style={
        {
          '--tab-color': color,
        } as React.CSSProperties
      }
    >
      {children}
    </div>
  );
}

function TabTitle({ children }: { children: React.ReactNode }) {
  return <span className="text-[11px]">{children}</span>;
}

export default function Layout({ children }: LayoutProps<'/'>) {
  return (
    // @ts-ignore
    <DocsLayout
      tree={source.pageTree}
      sidebar={{
        defaultOpenLevel: 1,
        prefetch: true,
        tabs: [
          {
            title: 'OpenIsle 前端',
            description: <TabTitle>前端开发文档</TabTitle>,
            url: '/frontend',
            icon: (
              <TabIcon color="#4ca154">
                <CompassIcon />
              </TabIcon>
            ),
          },
          {
            title: 'OpenIsle 后端',
            description: <TabTitle>后端开发文档</TabTitle>,
            url: '/backend',
            icon: (
              <TabIcon color="#1f66f4">
                <ServerIcon />
              </TabIcon>
            ),
          },
          {
            title: 'OpenIsle API',
            description: <TabTitle>后端 API 文档</TabTitle>,
            url: '/openapi',
            icon: (
              <TabIcon color="#677489">
                <CodeXmlIcon />
              </TabIcon>
            ),
          },
        ],
      }}
      {...baseOptions()}
    >
      {children}
    </DocsLayout>
  );
}
