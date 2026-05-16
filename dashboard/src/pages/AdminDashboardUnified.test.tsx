import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AdminDashboardUnified from '../pages/AdminDashboardUnified';

describe('AdminDashboardUnified', () => {
  it('renders without crashing', () => {
    render(<AdminDashboardUnified />);
    expect(screen.getByText(/supremeai/i)).toBeInTheDocument();
  });
});