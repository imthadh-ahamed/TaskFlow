import { APP_NAME } from "@/constants/app";

export default function Home() {
  return (
    <main id="main-content" className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-12 text-center sm:px-16 sm:py-16">
      <h1 className="text-4xl font-semibold tracking-tight">{APP_NAME}</h1>
      <p className="text-zinc-500 dark:text-zinc-400">
        Frontend scaffold — features coming soon.
      </p>
    </main>
  );
}
