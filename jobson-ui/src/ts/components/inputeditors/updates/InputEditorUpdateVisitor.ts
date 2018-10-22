export interface InputEditorUpdateVisitor<T> {
    visitValue: (value: any) => T;
    visitMissing: () => T;
    visitErrors: (errors: string[]) => T;
}
