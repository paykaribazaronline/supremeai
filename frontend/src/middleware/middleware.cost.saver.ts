
// middleware.ts - Save Costs at the Edge
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Paths that should be cached aggressively
const CACHEABLE_PATHS = [
  '/api/health',
  '/api/status',
  '/api/config',
  '/',
  '/about',
  '/pricing',
  '/docs',
];

// Paths that should have strict rate limiting
const RATE_LIMITED_PATHS = [
  '/api/ai/',
  '/api/generate',
  '/api/chat',
  '/api/complete',
];

// In-memory rate limiting (for demo; use Redis in production)
const rateLimitMap = new Map<string, { count: number; resetTime: number }>();
const RATE_LIMIT = {
  requests: 10,      // Max requests per window
  windowMs: 60000,   // 1 minute window
};

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const ip = request.ip || request.headers.get('x-forwarded-for') || 'unknown';

  // ─── Response Caching for GET Requests ──────
  if (request.method === 'GET' && CACHEABLE_PATHS.some(path => pathname.startsWith(path))) {
    const cacheKey = `middleware-cache:${pathname}`;
    
    // Add cache headers
    const response = NextResponse.next();
    response.headers.set('Cache-Control', 'public, s-maxage=300, stale-while-revalidate=60');
    response.headers.set('CDN-Cache-Control', 'public, s-maxage=3600');
    
    return response;
  }

  // ─── Rate Limiting for AI Endpoints ──────────
  if (RATE_LIMITED_PATHS.some(path => pathname.includes(path))) {
    const now = Date.now();
    const limitData = rateLimitMap.get(ip);

    if (!limitData || now > limitData.resetTime) {
      // New window
      rateLimitMap.set(ip, { count: 1, resetTime: now + RATE_LIMIT.windowMs });
    } else if (limitData.count >= RATE_LIMIT.requests) {
      // Rate limited!
      return new NextResponse(
        JSON.stringify({ error: 'Rate limit exceeded. Please try again later.' }),
        { status: 429, headers: { 'Content-Type': 'application/json' } }
      );
    } else {
      limitData.count++;
    }
  }

  // ─── Request Size Limitation ─────────────────
  const contentLength = parseInt(request.headers.get('content-length') || '0');
  if (contentLength > 1024 * 1024) { // 1MB limit
    return new NextResponse(
      JSON.stringify({ error: 'Payload too large. Max size: 1MB.' }),
      { status: 413, headers: { 'Content-Type': 'application/json' } }
    );
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
};
