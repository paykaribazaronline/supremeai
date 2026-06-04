// Utility function for formatting model lists for display
export const formatModelList = (models: string[] | undefined): string => {
  if (!models || models.length === 0) {
    return "-";
  }

  const displayedModels = models.slice(0, 3);
  const remainingCount = models.length > 3 ? models.length - 3 : 0;

  if (remainingCount > 0) {
    return `${displayedModels.join(", ")} +${remainingCount} more`;
  }

  return displayedModels.join(", ");
};

// Utility function to validate URL format
export const isValidUrl = (urlString: string): boolean => {
  try {
    const url = new URL(urlString);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch (_) {
    return false;
  }
};
