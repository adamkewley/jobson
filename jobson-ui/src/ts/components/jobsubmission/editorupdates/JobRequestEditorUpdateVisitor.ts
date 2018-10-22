export interface JobRequestEditorUpdateVisitor<T> {
    visitValue(value: any): T;
    visitErrors(errors: string[]): T;
}
