import { createElement } from 'react';
import { icons } from 'lucide-react';
import { loader } from 'fumadocs-core/source';
import { transformerOpenAPI } from 'fumadocs-openapi/server';
import { createOpenAPI } from 'fumadocs-openapi/server';
import { docs } from '@/.source';
import * as Adapters from './media-adapter';
import * as ClientAdapters from './media-adapter.client';

// See https://fumadocs.vercel.app/docs/headless/source-api for more info
export const source = loader({
  // it assigns a URL to your pages
  baseUrl: '/',
  source: docs.toFumadocsSource(),
  pageTree: {
    transformers: [transformerOpenAPI()],
  },
  icon(icon) {
    if (!icon) {
      return;
    }
    if (icon in icons) {
      return createElement(icons[icon as keyof typeof icons]);
    }
  },
});

export const openapi = createOpenAPI({
  proxyUrl: '/api/proxy',
  mediaAdapters: {
    // override the default adapter of `application/json`
    'application/json': {
      ...Adapters.OpenIsleMediaAdapter,
      client: ClientAdapters.OpenIsleMediaAdapter,
    },
  },
});
