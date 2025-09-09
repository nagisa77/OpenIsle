import type { BaseLayoutProps } from 'fumadocs-ui/layouts/shared';

/**
 * Shared layout configurations
 */
export function baseOptions(): BaseLayoutProps {
  return {
    githubUrl: 'https://github.com/nagisa77/OpenIsle',
    nav: {
      title: 'OpenIsle Docs',
      url: '/',
    },
    searchToggle: {
      enabled: false,
    },
    // see https://fumadocs.dev/docs/ui/navigation/links
    links: [],
  };
}
