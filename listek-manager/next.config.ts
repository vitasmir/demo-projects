import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  turbopack: {
    root: process.cwd(),
  },
  async rewrites() {
    return [{
      source: "/api/admin/:path*",
      destination: `${process.env.ADMIN_BACKEND_URL ?? "http://localhost:8090"}/api/admin/:path*`,
    }, {
      source: "/overview",
      destination: "/",
    }, {
      source: "/loans",
      destination: "/",
    }, {
      source: "/reports",
      destination: "/",
    }, {
      source: "/overdrafts",
      destination: "/",
    }, {
      source: "/clients",
      destination: "/",
    }, {
      source: "/settings",
      destination: "/",
    }, {
      source: "/admins",
      destination: "/",
    }];
  },
};

export default nextConfig;
