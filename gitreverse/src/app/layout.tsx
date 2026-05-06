import type { Metadata } from "next";
import { GeistSans } from "geist/font/sans";
import { GeistMono } from "geist/font/mono";
import "./styles/globals.css";

export const metadata: Metadata = {
  title: "GitReverse - Recreate Projects with AI",
  description:
    "Generate natural-language prompts from GitHub repos to recreate projects from scratch",
  openGraph: {
    title: "GitReverse",
    description: "Paste a repo, get a prompt to recreate it from scratch",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body
        className={`${GeistSans.variable} ${GeistMono.variable} font-sans antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
