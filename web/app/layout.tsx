import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ethereum Block Explorer — a focused chain reader",
  description:
    "Privately analyze your own compatible Ethereum CSV exports in the browser, or explore the bundled sample. Files never leave your device.",
  metadataBase: new URL("https://ethereum-blocks.vercel.app"),
  openGraph: {
    type: "website",
    siteName: "Ethereum Block Explorer",
    title: "Ethereum Block Explorer — a focused chain reader",
    description:
      "Privately analyze your own compatible Ethereum CSV exports in the browser. Files never leave your device.",
    images: ["/og.svg"]
  },
  twitter: {
    card: "summary_large_image",
    title: "Ethereum Block Explorer",
    description: "Privately analyze your own compatible Ethereum CSV exports in the browser.",
    images: ["/og.svg"]
  },
  icons: { icon: "/favicon.svg" }
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#fafbfd" },
    { media: "(prefers-color-scheme: dark)", color: "#06070b" }
  ]
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var t=localStorage.getItem("theme");if(t==="dark"||(!t&&window.matchMedia("(prefers-color-scheme: dark)").matches)){document.documentElement.dataset.theme="dark";}}catch(e){}})();`
          }}
        />
      </head>
      <body>{children}</body>
    </html>
  );
}
