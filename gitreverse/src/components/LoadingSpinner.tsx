"use client";

interface LoadingSpinnerProps {
  flavorText: string;
}

export default function LoadingSpinner({ flavorText }: LoadingSpinnerProps) {
  return (
    <div className="text-center py-16">
      {/* Animated Spinner */}
      <div className="inline-block relative">
        <div className="w-16 h-16 border-4 border-accent/30 rounded-full"></div>
        <div className="absolute top-0 left-0 w-16 h-16 border-4 border-transparent border-t-accent rounded-full animate-spin"></div>
      </div>

      {/* Rotating Flavor Text */}
      <p className="mt-6 text-lg text-gray-600 animate-pulse font-medium">
        {flavorText}
      </p>

      {/* Progress Dots */}
      <div className="mt-4 flex justify-center gap-2">
        <div className="w-2 h-2 bg-accent rounded-full animate-bounce"></div>
        <div className="w-2 h-2 bg-accent rounded-full animate-bounce delay-100"></div>
        <div className="w-2 h-2 bg-accent rounded-full animate-bounce delay-200"></div>
      </div>
    </div>
  );
}
