export const FormState = {
  Creating: 'creating',
  Editing: 'editing',
} as const;

export type FormState = (typeof FormState)[keyof typeof FormState];
