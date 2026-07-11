# 📄 ফাইল: apps/studio-client/src/components/ui/Card.stories.tsx

**প্রকার:** .tsx  
**সাইজ:** 1,081 বাইট  
**আপডেট:** 2026-07-11T13:51:38.449933

---

## কোড

```tsx
import React from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from './Card';
import { Button } from './Button';
import { Input } from './Input';

const meta: Meta<typeof Card> = {
  title: 'Design System/UI/Card',
  component: Card,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof Card>;

export const Default: Story = {
  render: () => (
    <Card className="w-[350px]">
      <CardHeader>
        <CardTitle>Create project</CardTitle>
        <CardDescription>Deploy your new project in one-click.</CardDescription>
      </CardHeader>
      <CardContent>
        <form>
          <div className="grid w-full items-center gap-4">
            <Input label="Name" placeholder="Name of your project" />
          </div>
        </form>
      </CardContent>
      <CardFooter className="flex justify-between">
        <Button variant="ghost">Cancel</Button>
        <Button variant="primary">Deploy</Button>
      </CardFooter>
    </Card>
  ),
};

```