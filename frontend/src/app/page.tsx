import { APP_NAME } from "@/constants/app";

export default function Home() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-12 text-center sm:px-16 sm:py-16">
      <h1 className="text-4xl font-semibold tracking-tight">{APP_NAME}</h1>
      <p className="text-zinc-500 dark:text-zinc-400">
        Dashboard — features coming soon.
      </p>
    </div>
  );
}
